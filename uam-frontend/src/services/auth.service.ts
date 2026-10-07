import { apiClient } from "../lib/api-client.ts";
import { tokenStorage } from "../lib/token-storage.ts";
import { userService } from "./user.service.ts";
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  VertifyRequest,
  ChangePasswordRequest,
} from "../types/index.ts";

export const authService = {
  /**
   * Đăng nhập người dùng bằng JWT (POST /loginWithJwt)
   * Đồng thời đồng bộ vai trò từ GET /me để mở khóa quyền Admin
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const res = await apiClient<LoginResponse>("/loginWithJwt", {
      method: "POST",
      skipAuth: true,
      body: {
        userName: credentials.userName.trim(),
        password: credentials.password,
      },
    });

    if (res && res.accessToken) {
      tokenStorage.setTokens(res.accessToken, res.refreshToken);

      // Sync user profile & roles from GET /me since backend JWT doesn't embed roles claim
      try {
        const profile = await userService.getProfile();
        if (profile) {
          const rolesArray: string[] = Array.isArray(profile.roles)
            ? profile.roles
                .map((r) => (typeof r === "string" ? r : r.roleName || r.name || ""))
                .filter(Boolean)
            : [];

          tokenStorage.setUserProfile({
            id: profile.id,
            username: profile.username || credentials.userName.trim(),
            roles: rolesArray,
          });
        }
      } catch {
        // Non-blocking: continue login even if /me fails initially
      }
    }
    return res;
  },

  /**
   * Đăng ký tài khoản mới (POST /api/v1/auth/register)
   */
  async register(data: RegisterRequest): Promise<string> {
    const rawDob = data.dob;
    return await apiClient<string>("/api/v1/auth/register", {
      method: "POST",
      skipAuth: true,
      body: {
        userName: data.userName.trim(),
        email: data.email.trim(),
        password: data.password,
        phone: data.phone?.trim() || "",
        dob: rawDob && rawDob.trim() !== "" ? rawDob.trim() : null,
      },
    });
  },

  /**
   * Xác thực mã OTP kích hoạt tài khoản (POST /api/v1/auth/verify)
   */
  async verifyOtp(data: VertifyRequest): Promise<string> {
    return await apiClient<string>("/api/v1/auth/verify", {
      method: "POST",
      skipAuth: true,
      body: {
        userName: data.userName.trim(),
        otpCode: data.otpCode.trim(),
      },
    });
  },

  /**
   * Đổi mật khẩu cá nhân (PUT /changePassword)
   */
  async changePassword(data: ChangePasswordRequest): Promise<string> {
    const current = (data.currentPassword || data.oldPassword || "").trim();
    const newPwd = data.newPassword;
    const confirm = (data.confirmNewPassword || data.confirmPassword || data.newPassword).trim();

    return await apiClient<string>("/changePassword", {
      method: "PUT",
      body: {
        currentPassword: current,
        oldPassword: current,
        newPassword: newPwd,
        confirmNewPassword: confirm,
        confirmPassword: confirm,
      },
    });
  },

  /**
   * Đăng xuất hệ thống và thu hồi refresh token (POST /logout)
   */
  async logout(): Promise<void> {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await apiClient<void>("/logout", {
          method: "POST",
          skipAuth: true,
          body: { refreshToken },
        });
      }
    } catch {
      // Silently ignore server error on logout to guarantee client tokens are cleared
    } finally {
      tokenStorage.clearTokens();
    }
  },
};
