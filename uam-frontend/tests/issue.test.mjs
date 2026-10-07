import test from "node:test";
import assert from "node:assert/strict";

const originalFetch = globalThis.fetch;
const originalLocalStorage = globalThis.localStorage;

function setupLocalStorage() {
  const store = new Map();
  globalThis.localStorage = {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, val) => store.set(key, String(val)),
    removeItem: (key) => store.delete(key),
    clear: () => store.clear(),
  };
}

test.beforeEach(() => {
  setupLocalStorage();
  Object.defineProperty(globalThis, "window", {
    value: { location: { href: "", pathname: "/spaces" } },
    configurable: true,
  });
});

test.afterEach(() => {
  globalThis.fetch = originalFetch;
  globalThis.localStorage = originalLocalStorage;
  delete globalThis.window;
});

const { spaceService } = await import("../src/services/space.service.ts");
const { issueService } = await import("../src/services/issue.service.ts");
const { sprintService } = await import("../src/services/sprint.service.ts");

test("spaceService.createSpace posts payload to /api/v1/spaces", async () => {
  let capturedUrl = "";
  let capturedBody = null;

  globalThis.fetch = async (url, init) => {
    capturedUrl = String(url);
    capturedBody = JSON.parse(init.body);
    return new Response(
      JSON.stringify({
        id: 1,
        name: "Backend Space",
        key: "BACK",
        description: "Space desc",
      }),
      { status: 201, headers: { "Content-Type": "application/json" } }
    );
  };

  const space = await spaceService.createSpace({
    name: "Backend Space",
    key: "BACK",
    description: "Space desc",
  });

  assert.equal(capturedUrl, "/backend/api/v1/spaces");
  assert.equal(capturedBody.name, "Backend Space");
  assert.equal(capturedBody.key, "BACK");
  assert.equal(space.id, 1);
});

test("issueService.createIssue passes Idempotency-Key when provided", async () => {
  let capturedHeaders = null;
  let capturedBody = null;

  globalThis.fetch = async (url, init) => {
    capturedHeaders = new Headers(init.headers);
    capturedBody = JSON.parse(init.body);
    return new Response(
      JSON.stringify({
        id: 10,
        key: "PROJ-1",
        spaceId: 1,
        summary: "Login with OAuth",
        status: "TODO",
        priority: "HIGH",
        version: 0,
      }),
      { status: 201, headers: { "Content-Type": "application/json" } }
    );
  };

  const issue = await issueService.createIssue(
    {
      spaceId: 1,
      type: "STORY",
      summary: "Login with OAuth",
      priority: "HIGH",
    },
    "idemp-key-12345"
  );

  assert.equal(issue.key, "PROJ-1");
  // Headers object normalized in fetch
  assert.equal(capturedHeaders.get("idempotency-key"), "idemp-key-12345");
  assert.equal(capturedBody.summary, "Login with OAuth");
});

test("issueService.updateIssue sends explicit boolean presence flags for Jackson 3 compatibility", async () => {
  let capturedUrl = "";
  let capturedBody = null;

  globalThis.fetch = async (url, init) => {
    capturedUrl = String(url);
    capturedBody = JSON.parse(init.body);
    return new Response(
      JSON.stringify({
        id: 10,
        key: "PROJ-1",
        version: 1,
        priority: "HIGH",
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  };

  // Test updating priority
  const updatedPriority = await issueService.updateIssue(10, {
    version: 0,
    priority: "HIGH",
  });

  assert.equal(capturedUrl, "/backend/api/v1/issues/10");
  assert.equal(capturedBody.version, 0);
  assert.equal(capturedBody.priority, "HIGH");
  assert.equal(capturedBody.priorityPresent, true);
  assert.equal(capturedBody.summaryPresent, false);
  assert.equal(capturedBody.assigneeIdPresent, false);
  assert.equal(capturedBody.sprintIdPresent, false);
  assert.equal(updatedPriority.version, 1);

  // Test unassigning member (assigneeId: null)
  await issueService.updateIssue(10, {
    version: 1,
    assigneeId: null,
  });
  assert.equal(capturedBody.version, 1);
  assert.equal(capturedBody.assigneeId, null);
  assert.equal(capturedBody.assigneeIdPresent, true);
  assert.equal(capturedBody.priorityPresent, false);

  // Test moving to Backlog (sprintId: null)
  await issueService.updateIssue(10, {
    version: 2,
    sprintId: null,
  });
  assert.equal(capturedBody.version, 2);
  assert.equal(capturedBody.sprintId, null);
  assert.equal(capturedBody.sprintIdPresent, true);
  assert.equal(capturedBody.priorityPresent, false);
});

test("issueService.changeStatus calls /api/v1/issues/:id/status with targetStatus and version", async () => {
  let capturedUrl = "";
  let capturedBody = null;

  globalThis.fetch = async (url, init) => {
    capturedUrl = String(url);
    capturedBody = JSON.parse(init.body);
    return new Response(
      JSON.stringify({
        id: 10,
        key: "PROJ-1",
        status: "DONE",
        version: 1,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  };

  const updated = await issueService.changeStatus(10, "DONE", 0);
  assert.equal(capturedUrl, "/backend/api/v1/issues/10/status");
  assert.deepEqual(capturedBody, { targetStatus: "DONE", version: 0 });
  assert.equal(updated.status, "DONE");
});

test("sprintService.completeSprint calls /api/v1/sprints/:id/complete with optimistic version & spaceRevision", async () => {
  let capturedUrl = "";
  let capturedBody = null;

  globalThis.fetch = async (url, init) => {
    capturedUrl = String(url);
    capturedBody = JSON.parse(init.body);
    return new Response(
      JSON.stringify({
        sourceSprintId: 5,
        sourceStatus: "CLOSED",
        sourceVersion: 2,
        targetSprintId: 6,
        movedIssueCount: 2,
        retainedIssueCount: 3,
        spaceRevision: 15,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  };

  const result = await sprintService.completeSprint(5, {
    version: 1,
    spaceRevision: 14,
    targetSprintId: 6,
  });

  assert.equal(capturedUrl, "/backend/api/v1/sprints/5/complete");
  assert.equal(capturedBody.version, 1);
  assert.equal(capturedBody.spaceRevision, 14);
  assert.equal(capturedBody.targetSprintId, 6);
  assert.equal(result.movedIssueCount, 2);
});
