import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import { http, HttpError } from "../src/lib/http.ts";

const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");

before(() => {
  Object.defineProperty(globalThis, "window", { value: {}, configurable: true });
});

after(() => {
  if (originalWindow) Object.defineProperty(globalThis, "window", originalWindow);
  else delete globalThis.window;
});

test("routes browser requests through /backend and returns JSON, status and headers", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/me?include=roles");
    assert.equal(options.body, undefined);
    assert.equal(options.headers.has("content-type"), false);
    return new Response('{"id":7}', {
      status: 200,
      headers: { "content-type": "application/json; charset=utf-8", "x-request-id": "request-1" },
    });
  });

  const result = await http("/me?include=roles");
  assert.deepEqual(result.data, { id: 7 });
  assert.equal(result.status, 200);
  assert.equal(result.headers.get("x-request-id"), "request-1");
});

test("serializes JSON bodies, preserves method, supplied headers and signal", async (t) => {
  const controller = new AbortController();
  const headers = new Headers({ "x-request-id": "request-2", Authorization: "Bearer test-token" });
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/me");
    assert.equal(options.method, "PATCH");
    assert.equal(options.body, '{"displayName":"Example"}');
    assert.equal(options.headers.get("content-type"), "application/json");
    assert.equal(options.headers.get("x-request-id"), "request-2");
    assert.equal(options.headers.get("authorization"), "Bearer test-token");
    assert.equal(options.signal, controller.signal);
    assert.equal(options.credentials, "include");
    return new Response(null, { status: 204 });
  });

  await http("/me", {
    method: "PATCH",
    body: { displayName: "Example" },
    headers,
    signal: controller.signal,
    credentials: "include",
  });
  assert.equal(headers.has("content-type"), false, "caller-owned headers must not be mutated");
});

test("respects explicit JSON content types and serializes null", async (t) => {
  t.mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.headers.get("content-type"), "application/merge-patch+json");
    assert.equal(options.body, "null");
    return new Response(null, { status: 204 });
  });

  await http("/me", {
    method: "PATCH",
    body: null,
    headers: { "Content-Type": "application/merge-patch+json" },
  });
});

test("returns plain text and empty responses without JSON parsing errors", async (t) => {
  const responses = [
    new Response("ready", { headers: { "content-type": "text/plain" } }),
    new Response(null, { status: 204 }),
    new Response(null, { status: 205 }),
    new Response("", { status: 200, headers: { "content-type": "application/json" } }),
  ];
  t.mock.method(globalThis, "fetch", async () => responses.shift());

  assert.equal((await http("/status")).data, "ready");
  assert.equal((await http("/status")).data, null);
  assert.equal((await http("/status")).data, null);
  assert.equal((await http("/status")).data, null);
});

test("HTTP errors preserve status, JSON error body and response headers", async (t) => {
  t.mock.method(globalThis, "fetch", async () => new Response('{"detail":"Not authorized"}', {
    status: 401,
    statusText: "Unauthorized",
    headers: { "content-type": "application/problem+json", "x-request-id": "request-3" },
  }));

  await assert.rejects(http("/me"), (error) => {
    assert.ok(error instanceof HttpError);
    assert.equal(error.name, "HttpError");
    assert.equal(error.message, "HTTP 401 Unauthorized");
    assert.equal(error.status, 401);
    assert.deepEqual(error.data, { detail: "Not authorized" });
    assert.equal(error.headers.get("x-request-id"), "request-3");
    return true;
  });
});

test("text, malformed JSON and empty error bodies retain HTTP status", async (t) => {
  const responses = [
    new Response("Unavailable", { status: 503, headers: { "content-type": "text/plain" } }),
    new Response("{invalid", { status: 502, headers: { "content-type": "application/json" } }),
    new Response(null, { status: 403 }),
  ];
  t.mock.method(globalThis, "fetch", async () => responses.shift());

  for (const [status, data] of [[503, "Unavailable"], [502, "{invalid"], [403, null]]) {
    await assert.rejects(http("/me"), (error) => {
      assert.ok(error instanceof HttpError);
      assert.equal(error.status, status);
      assert.equal(error.data, data);
      return true;
    });
  }
});

test("network and abort failures propagate without pretending to be HTTP errors", async (t) => {
  const failures = [new TypeError("Network unavailable"), new DOMException("Cancelled", "AbortError")];
  let failure;
  t.mock.method(globalThis, "fetch", async () => { throw failure; });

  for (failure of failures) {
    await assert.rejects(http("/me"), (error) => error === failure && !(error instanceof HttpError));
  }
});

test("rejects paths that could escape the backend prefix before calling fetch", async (t) => {
  const fetchMock = t.mock.method(globalThis, "fetch", async () => new Response());
  const invalidPaths = [
    "https://example.com/me", "//example.com/me", "me", "/../me", "/./me",
    "/%2e%2e/me", "/nested/../../me", "/nested%2f..%2fme", "/\\example.com",
    "/%5cme", "/%252e%252e/me", "/bad%encoding", "/me#fragment", "/me\n",
  ];
  for (const path of invalidPaths) await assert.rejects(http(path), TypeError);
  assert.equal(fetchMock.mock.callCount(), 0);
});

test("allows encoded path values and URLs inside query parameters", async (t) => {
  const path = "/names/%E1%BB%87?returnTo=https%3A%2F%2Fexample.com%2F";
  t.mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, `/backend${path}`);
    return new Response(null, { status: 204 });
  });
  await http(path);
});

test("rejects accidental server-side calls at invocation time", async (t) => {
  const fetchMock = t.mock.method(globalThis, "fetch", async () => new Response());
  delete globalThis.window;
  try {
    await assert.rejects(http("/me"), /browser-only/);
    assert.equal(fetchMock.mock.callCount(), 0);
  } finally {
    Object.defineProperty(globalThis, "window", { value: {}, configurable: true });
  }
});
