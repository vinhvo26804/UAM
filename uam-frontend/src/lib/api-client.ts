import { http, HttpError } from "./http.ts";
import type { HttpOptions } from "./http.ts";
import { tokenStorage } from "./token-storage.ts";
import type { LoginResponse } from "../types/index.ts";

export class ApiError extends Error {
  readonly status: number;
  readonly data: unknown;

  constructor(message: string, status: number, data?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

// Queue mechanism to prevent duplicate concurrent refresh requests
let isRefreshing = false;
let refreshSubscribers: Array<(newToken: string | null) => void> = [];

function subscribeTokenRefresh(cb: (newToken: string | null) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(newToken: string | null) {
  refreshSubscribers.forEach((cb) => cb(newToken));
  refreshSubscribers = [];
}

/**
 * Perform token refresh request to backend /refreshToken
 */
async function performRefreshToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await http<LoginResponse>("/refreshToken", {
      method: "POST",
      body: { refreshToken },
    });

    const data = response.data as LoginResponse | null;
    if (data && data.accessToken) {
      tokenStorage.setTokens(data.accessToken, data.refreshToken || refreshToken);
      return data.accessToken;
    }
    return null;
  } catch {
    tokenStorage.clearTokens();
    return null;
  }
}

/**
 * Parse human readable error message from backend response
 */
export function extractErrorMessage(err: unknown, fallbackMessage = "Có lỗi xảy ra, vui lòng thử lại"): string {
  if (err instanceof ApiError) {
    return err.message;
  }
  if (err instanceof HttpError) {
    if (typeof err.data === "string" && err.data.trim()) {
      return err.data;
    }
    if (err.data && typeof err.data === "object") {
      const obj = err.data as Record<string, unknown>;
      if (typeof obj.error === "string" && obj.error) return obj.error;
      if (typeof obj.message === "string" && obj.message) return obj.message;
      if (typeof obj.detail === "string" && obj.detail) return obj.detail;
    }
    if (err.status === 401) return "Phiên đăng nhập đã hết hạn hoặc không có quyền truy cập.";
    if (err.status === 403) return "Bạn không có quyền thực hiện thao tác này.";
    if (err.status === 404) return "Không tìm thấy dữ liệu yêu cầu.";
    if (err.status >= 500) return "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
  }
  if (err && typeof err === "object" && "status" in err) {
    const s = (err as { status: number }).status;
    if (s === 401) return "Phiên đăng nhập đã hết hạn hoặc không có quyền truy cập.";
    if (s === 403) return "Bạn không có quyền thực hiện thao tác này.";
    if (s === 404) return "Không tìm thấy dữ liệu yêu cầu.";
    if (s >= 500) return "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
  }
  if (err instanceof Error) {
    return err.message;
  }
  return fallbackMessage;
}

export interface ApiClientOptions extends HttpOptions {
  skipAuth?: boolean;
}

/**
 * Enterprise API Client with automatic Bearer token injection,
 * concurrency-safe refresh token queuing, and error translation.
 */
export async function apiClient<T = unknown>(
  path: string,
  options: ApiClientOptions = {}
): Promise<T> {
  const { skipAuth, headers: customHeaders, ...fetchOptions } = options;
  const headers = new Headers(customHeaders);

  // Attach token if not skipped
  if (!skipAuth) {
    const accessToken = tokenStorage.getAccessToken();
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  try {
    const res = await http<T>(path, { ...fetchOptions, headers });

    // Special handling for auth verify endpoint which may return 200 with error text
    if (path === "/api/v1/auth/verify" && typeof res.data === "string") {
      if (res.data !== "Verify Success") {
        let msg = "Xác thực mã OTP thất bại.";
        if (res.data === "OTP expired!") msg = "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.";
        if (res.data === "OTP not found") msg = "Mã OTP không tìm thấy hoặc đã được kích hoạt.";
        if (res.data === "Not valid") msg = "Thông tin xác thực không hợp lệ.";
        throw new ApiError(msg, 400, res.data);
      }
    }

    return res.data as T;
  } catch (error) {
    if (error instanceof HttpError) {
      // 403 Forbidden: DO NOT loop or refresh, reject immediately as specified
      if (error.status === 403) {
        throw new ApiError(
          extractErrorMessage(error, "Bạn không có quyền thực hiện thao tác này."),
          403,
          error.data
        );
      }

      // 401 Unauthorized: Attempt token refresh (only if we had a token and this is not a refresh/login call)
      const isAuthCall = path.includes("/login") || path.includes("/refreshToken") || path.includes("/logout");
      if (error.status === 401 && !skipAuth && !isAuthCall) {
        if (!isRefreshing) {
          isRefreshing = true;
          const newAccessToken = await performRefreshToken();
          isRefreshing = false;
          onRefreshed(newAccessToken);

          if (newAccessToken) {
            headers.set("Authorization", `Bearer ${newAccessToken}`);
            const retryRes = await http<T>(path, { ...fetchOptions, headers });
            return retryRes.data as T;
          } else {
            tokenStorage.clearTokens();
            if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
              // eslint-disable-next-line @next/next/no-location-assign-relative-destination
              window.location.href = `/login?session_expired=1`;
            }
            throw new ApiError("Phiên đăng nhập đã hết hạn.", 401, error.data);
          }
        } else {
          // Another refresh is in flight, queue this request
          return new Promise<T>((resolve, reject) => {
            subscribeTokenRefresh(async (newToken) => {
              if (newToken) {
                try {
                  headers.set("Authorization", `Bearer ${newToken}`);
                  const retryRes = await http<T>(path, { ...fetchOptions, headers });
                  resolve(retryRes.data as T);
                } catch (retryErr) {
                  reject(retryErr);
                }
              } else {
                reject(new ApiError("Phiên đăng nhập đã hết hạn.", 401, error.data));
              }
            });
          });
        }
      }

      throw new ApiError(extractErrorMessage(error), error.status, error.data);
    }

    throw error;
  }
}
