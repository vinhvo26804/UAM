import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { apiClient, ApiError, extractErrorMessage } from "../src/lib/api-client.ts";
import { tokenStorage } from "../src/lib/token-storage.ts";

class MockLocalStorage {
  constructor() {
    this.store = new Map();
  }
  getItem(key) {
    return this.store.get(key) || null;
  }
  setItem(key, value) {
    this.store.set(key, String(value));
  }
  removeItem(key) {
    this.store.delete(key);
  }
  clear() {
    this.store.clear();
  }
}

let mockStorage;

beforeEach(() => {
  mockStorage = new MockLocalStorage();
  Object.defineProperty(globalThis, "window", {
    value: { location: { href: "", pathname: "/dashboard" } },
    configurable: true,
  });
  Object.defineProperty(globalThis, "localStorage", {
    value: mockStorage,
    configurable: true,
  });
});

afterEach(() => {
  delete globalThis.window;
  delete globalThis.localStorage;
});

test("attaches Bearer access token when available and skipAuth is not set", async (t) => {
  tokenStorage.setTokens("my-access-token", "my-refresh-token");

  t.mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.headers.get("authorization"), "Bearer my-access-token");
    return new Response(JSON.stringify({ username: "testuser" }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  });

  const res = await apiClient("/me");
  assert.deepEqual(res, { username: "testuser" });
});

test("omits Authorization header when skipAuth is true", async (t) => {
  tokenStorage.setTokens("my-access-token", "my-refresh-token");

  t.mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.headers.has("authorization"), false);
    return new Response(JSON.stringify({ message: "public" }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  });

  const res = await apiClient("/public", { skipAuth: true });
  assert.deepEqual(res, { message: "public" });
});

test("HTTP 403 Forbidden rejects immediately without attempting refresh", async (t) => {
  tokenStorage.setTokens("my-access-token", "my-refresh-token");

  let refreshCalled = false;
  t.mock.method(globalThis, "fetch", async (url) => {
    if (url.includes("/refreshToken")) {
      refreshCalled = true;
      return new Response(JSON.stringify({ accessToken: "new-token" }), { status: 200 });
    }
    return new Response(JSON.stringify({ error: "Access Denied" }), {
      status: 403,
      headers: { "content-type": "application/json" },
    });
  });

  await assert.rejects(apiClient("/api/admin/users"), (err) => {
    assert.ok(err instanceof ApiError);
    assert.equal(err.status, 403);
    assert.equal(refreshCalled, false, "Must NEVER call refreshToken on 403 Forbidden");
    return true;
  });
});

test("HTTP 401 triggers token refresh and retries the original request successfully", async (t) => {
  tokenStorage.setTokens("old-access-token", "valid-refresh-token");

  let attempts = 0;
  t.mock.method(globalThis, "fetch", async (url, options) => {
    if (url === "/backend/refreshToken") {
      assert.equal(options.method, "POST");
      return new Response(
        JSON.stringify({
          accessToken: "new-access-token",
          refreshToken: "new-refresh-token",
        }),
        {
          status: 200,
          headers: { "content-type": "application/json" },
        }
      );
    }

    if (url === "/backend/me") {
      attempts++;
      if (attempts === 1) {
        assert.equal(options.headers.get("authorization"), "Bearer old-access-token");
        return new Response(JSON.stringify({ error: "Token expired" }), {
          status: 401,
          headers: { "content-type": "application/json" },
        });
      } else {
        assert.equal(options.headers.get("authorization"), "Bearer new-access-token");
        return new Response(JSON.stringify({ id: 1, username: "john" }), {
          status: 200,
          headers: { "content-type": "application/json" },
        });
      }
    }

    throw new Error(`Unexpected url: ${url}`);
  });

  const res = await apiClient("/me");
  assert.deepEqual(res, { id: 1, username: "john" });
  assert.equal(tokenStorage.getAccessToken(), "new-access-token");
  assert.equal(tokenStorage.getRefreshToken(), "new-refresh-token");
});

test("catches backend 200 text errors on /api/v1/auth/verify and translates them to ApiError", async (t) => {
  t.mock.method(globalThis, "fetch", async () => {
    return new Response("OTP expired!", {
      status: 200,
      headers: { "content-type": "text/plain" },
    });
  });

  await assert.rejects(
    apiClient("/api/v1/auth/verify", {
      method: "POST",
      skipAuth: true,
      body: { userName: "test", otpCode: "123456" },
    }),
    (err) => {
      assert.ok(err instanceof ApiError);
      assert.equal(err.status, 400);
      assert.match(err.message, /hết hạn/);
      return true;
    }
  );
});

test("extractErrorMessage extracts correct Vietnamese error strings", () => {
  assert.equal(extractErrorMessage(new ApiError("Custom Error", 400)), "Custom Error");
  assert.equal(
    extractErrorMessage({ status: 401 }),
    "Phiên đăng nhập đã hết hạn hoặc không có quyền truy cập."
  );
  assert.equal(
    extractErrorMessage({ status: 403 }),
    "Bạn không có quyền thực hiện thao tác này."
  );
  assert.equal(
    extractErrorMessage({ status: 404 }),
    "Không tìm thấy dữ liệu yêu cầu."
  );
  assert.equal(
    extractErrorMessage({ status: 500 }),
    "Lỗi máy chủ nội bộ. Vui lòng thử lại sau."
  );
});
