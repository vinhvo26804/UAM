import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { tokenStorage } from "../src/lib/token-storage.ts";

// In-memory mock localStorage for Node.js test environment
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
  Object.defineProperty(globalThis, "window", { value: {}, configurable: true });
  Object.defineProperty(globalThis, "localStorage", { value: mockStorage, configurable: true });
});

afterEach(() => {
  delete globalThis.window;
  delete globalThis.localStorage;
});

function createMockJwt(payload) {
  const header = btoa(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = btoa(JSON.stringify(payload));
  const signature = "mock-signature";
  return `${header}.${body}.${signature}`;
}

test("stores, retrieves, and clears tokens in localStorage", () => {
  tokenStorage.setTokens("access-123", "refresh-456");
  assert.equal(tokenStorage.getAccessToken(), "access-123");
  assert.equal(tokenStorage.getRefreshToken(), "refresh-456");

  tokenStorage.setAccessToken("new-access");
  assert.equal(tokenStorage.getAccessToken(), "new-access");

  tokenStorage.setRefreshToken("new-refresh");
  assert.equal(tokenStorage.getRefreshToken(), "new-refresh");

  tokenStorage.clearTokens();
  assert.equal(tokenStorage.getAccessToken(), null);
  assert.equal(tokenStorage.getRefreshToken(), null);
});

test("decodes JWT payload accurately", () => {
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const token = createMockJwt({
    sub: "testuser",
    roles: ["ROLE_USER", "ROLE_ADMIN"],
    exp: futureExp,
  });

  const claims = tokenStorage.decodeToken(token);
  assert.ok(claims);
  assert.equal(claims.sub, "testuser");
  assert.deepEqual(claims.roles, ["ROLE_USER", "ROLE_ADMIN"]);
  assert.equal(claims.exp, futureExp);
});

test("handles malformed token gracefully without throwing", () => {
  assert.equal(tokenStorage.decodeToken(null), null);
  assert.equal(tokenStorage.decodeToken(""), null);
  assert.equal(tokenStorage.decodeToken("invalid.token"), null);
  assert.equal(tokenStorage.decodeToken("a.b.c"), null);
});

test("getCurrentUser returns user information for valid active token", () => {
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const token = createMockJwt({
    sub: "alice",
    roles: ["ROLE_USER"],
    exp: futureExp,
  });
  tokenStorage.setAccessToken(token);

  const user = tokenStorage.getCurrentUser();
  assert.ok(user);
  assert.equal(user.username, "alice");
  assert.deepEqual(user.roles, ["ROLE_USER"]);
  assert.equal(tokenStorage.isAdmin(), false);
});

test("getCurrentUser rejects expired token", () => {
  const pastExp = Math.floor(Date.now() / 1000) - 3600; // expired 1 hour ago
  const token = createMockJwt({
    sub: "bob",
    roles: ["ROLE_USER"],
    exp: pastExp,
  });
  tokenStorage.setAccessToken(token);

  assert.equal(tokenStorage.getCurrentUser(), null);
});

test("isAdmin accurately identifies administrative privileges", () => {
  const futureExp = Math.floor(Date.now() / 1000) + 3600;

  // With ROLE_ADMIN in array
  const adminToken = createMockJwt({
    sub: "adminUser",
    roles: ["ROLE_USER", "ROLE_ADMIN"],
    exp: futureExp,
  });
  tokenStorage.setAccessToken(adminToken);
  assert.equal(tokenStorage.isAdmin(), true);

  // Without ROLE_ADMIN
  const normalToken = createMockJwt({
    sub: "normalUser",
    roles: ["ROLE_USER"],
    exp: futureExp,
  });
  tokenStorage.setAccessToken(normalToken);
  assert.equal(tokenStorage.isAdmin(), false);
});
