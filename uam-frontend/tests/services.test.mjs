import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { authService } from "../src/services/auth.service.ts";
import { userService } from "../src/services/user.service.ts";
import { adminService } from "../src/services/admin.service.ts";
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

// --- AUTH SERVICE TESTS ---
test("authService.login sends credentials and stores tokens", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/loginWithJwt");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.userName, "admin");
    assert.equal(body.password, "Secret@123");
    return new Response(
      JSON.stringify({ accessToken: "jwt-access-token", refreshToken: "jwt-refresh-token" }),
      { status: 200, headers: { "content-type": "application/json" } }
    );
  });

  const res = await authService.login({ userName: "admin", password: "Secret@123" });
  assert.equal(res.accessToken, "jwt-access-token");
  assert.equal(tokenStorage.getAccessToken(), "jwt-access-token");
  assert.equal(tokenStorage.getRefreshToken(), "jwt-refresh-token");
});

test("authService.register sends accurate registration DTO", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/auth/register");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.userName, "newuser");
    assert.equal(body.email, "user@example.com");
    return new Response("User registered successfully", {
      status: 200,
      headers: { "content-type": "text/plain" },
    });
  });

  const res = await authService.register({
    userName: "newuser",
    email: "user@example.com",
    password: "Password@123",
  });
  assert.equal(res, "User registered successfully");
});

test("authService.logout clears tokens even if backend call fails", async (t) => {
  tokenStorage.setTokens("acc", "ref");

  t.mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, "/backend/logout");
    return new Response(null, { status: 500 });
  });

  await authService.logout();
  assert.equal(tokenStorage.getAccessToken(), null);
  assert.equal(tokenStorage.getRefreshToken(), null);
});

// --- USER SERVICE TESTS ---
test("userService.getProfile calls /me with Bearer token", async (t) => {
  tokenStorage.setTokens("user-token", "ref");

  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/me");
    assert.equal(options.headers.get("authorization"), "Bearer user-token");
    return new Response(
      JSON.stringify({
        id: 10,
        username: "johndoe",
        email: "john@example.com",
        fullname: "John Doe",
        phone: "0912345678",
      }),
      { status: 200, headers: { "content-type": "application/json" } }
    );
  });

  const profile = await userService.getProfile();
  assert.equal(profile.id, 10);
  assert.equal(profile.username, "johndoe");
  assert.equal(profile.fullname, "John Doe");
});

test("userService.updateProfile calls /me/update with payload", async (t) => {
  tokenStorage.setTokens("user-token", "ref");

  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/me/update");
    assert.equal(options.method, "PUT");
    const body = JSON.parse(options.body);
    assert.equal(body.fullname, "Jane Doe");
    return new Response(
      JSON.stringify({ id: 10, username: "johndoe", fullname: "Jane Doe" }),
      { status: 200, headers: { "content-type": "application/json" } }
    );
  });

  const res = await userService.updateProfile({ fullname: "Jane Doe" });
  assert.equal(res.fullname, "Jane Doe");
});

test("userService.requestChangeEmail and verifyChangeEmail calls respective endpoints", async (t) => {
  tokenStorage.setTokens("user-token", "ref");

  t.mock.method(globalThis, "fetch", async (url, options) => {
    if (url === "/backend/me/email") {
      assert.equal(options.method, "PUT");
      const body = JSON.parse(options.body);
      assert.equal(body.newEmail, "new@mail.com");
      return new Response("OTP Sent", { status: 200, headers: { "content-type": "text/plain" } });
    }
    if (url === "/backend/me/email/verify") {
      assert.equal(options.method, "POST");
      const body = JSON.parse(options.body);
      assert.equal(body.otp, "654321");
      return new Response("Email changed", { status: 200, headers: { "content-type": "text/plain" } });
    }
    throw new Error(`Unexpected url ${url}`);
  });

  const reqRes = await userService.requestChangeEmail({ newEmail: "new@mail.com" });
  assert.equal(reqRes, "OTP Sent");

  const verRes = await userService.verifyChangeEmail({ otp: "654321" });
  assert.equal(verRes, "Email changed");
});

// --- ADMIN SERVICE TESTS ---
test("adminService.searchUsers constructs correct query parameters", async (t) => {
  tokenStorage.setTokens("admin-token", "ref");

  t.mock.method(globalThis, "fetch", async (url) => {
    assert.equal(
      url,
      "/backend/api/admin/users?username=admin&email=admin%40corp.com&role=ROLE_ADMIN&status=ACTIVE"
    );
    return new Response(JSON.stringify([{ id: 1, username: "admin", status: "ACTIVE" }]), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  });

  const users = await adminService.searchUsers({
    username: "admin",
    email: "admin@corp.com",
    role: "ROLE_ADMIN",
    status: "ACTIVE",
  });
  assert.equal(users.length, 1);
  assert.equal(users[0].username, "admin");
});

test("adminService status mutations call proper PATCH and DELETE endpoints", async (t) => {
  tokenStorage.setTokens("admin-token", "ref");

  const calls = [];
  t.mock.method(globalThis, "fetch", async (url, options) => {
    calls.push({ url, method: options.method });
    return new Response(JSON.stringify({ id: 5, status: "UPDATED" }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  });

  await adminService.lockUser(5);
  await adminService.unlockUser(5);
  await adminService.disableUser(5);
  await adminService.activateUser(5);
  await adminService.deleteUser(5);

  assert.deepEqual(calls, [
    { url: "/backend/api/admin/users/5/lock", method: "PATCH" },
    { url: "/backend/api/admin/users/5/unlock", method: "PATCH" },
    { url: "/backend/api/admin/users/5/disable", method: "PATCH" },
    { url: "/backend/api/admin/users/5/activate", method: "PATCH" },
    { url: "/backend/api/admin/users/5", method: "DELETE" },
  ]);
});

test("adminService role assignment and removal call correct endpoints", async (t) => {
  tokenStorage.setTokens("admin-token", "ref");

  t.mock.method(globalThis, "fetch", async (url, options) => {
    if (url === "/backend/api/roles/assign") {
      assert.equal(options.method, "POST");
      const body = JSON.parse(options.body);
      assert.equal(body.userName, "alice");
      assert.equal(body.roleName, "ROLE_ADMIN");
      return new Response("Assigned", { status: 200, headers: { "content-type": "text/plain" } });
    }
    if (url === "/backend/api/roles/remove") {
      assert.equal(options.method, "DELETE");
      const body = JSON.parse(options.body);
      assert.equal(body.userName, "alice");
      assert.equal(body.roleName, "ROLE_MANAGER");
      return new Response("Removed", { status: 200, headers: { "content-type": "text/plain" } });
    }
    throw new Error(`Unexpected: ${url}`);
  });

  const assignRes = await adminService.assignRole({ userName: "alice", roleName: "ROLE_ADMIN" });
  assert.equal(assignRes, "Assigned");

  const removeRes = await adminService.removeRole({ userName: "alice", roleName: "ROLE_MANAGER" });
  assert.equal(removeRes, "Removed");
});
