import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { teamService } from "../src/services/team.service.ts";
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
  tokenStorage.setTokens("fake-access-token", "fake-refresh-token");
});

afterEach(() => {
  delete globalThis.window;
  delete globalThis.localStorage;
});

test("teamService.createTeam sends trimmed name and description to /api/v1/teams", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams");
    assert.equal(options.method, "POST");
    const authHeader =
      typeof options.headers.get === "function"
        ? options.headers.get("Authorization")
        : options.headers["Authorization"];
    assert.equal(authHeader, "Bearer fake-access-token");
    const body = JSON.parse(options.body);
    assert.equal(body.name, "Nhóm Alpha");
    assert.equal(body.description, "Mô tả nhóm");

    return new Response(
      JSON.stringify({
        id: 1,
        name: "Nhóm Alpha",
        description: "Mô tả nhóm",
        currentUserRole: "OWNER",
        memberCount: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
      { status: 201, headers: { "Content-Type": "application/json" } }
    );
  });

  const team = await teamService.createTeam({
    name: "  Nhóm Alpha  ",
    description: "  Mô tả nhóm  ",
  });

  assert.equal(team.id, 1);
  assert.equal(team.name, "Nhóm Alpha");
  assert.equal(team.currentUserRole, "OWNER");
  assert.equal(team.memberCount, 1);
});

test("teamService.getMyTeams calls GET /api/v1/teams", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams");
    assert.equal(options.method, "GET");

    return new Response(
      JSON.stringify([
        {
          id: 1,
          name: "Nhóm Alpha",
          currentUserRole: "OWNER",
          memberCount: 1,
        },
      ]),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  });

  const teams = await teamService.getMyTeams();
  assert.equal(teams.length, 1);
  assert.equal(teams[0].name, "Nhóm Alpha");
});

test("teamService.getTeamDetail calls GET /api/v1/teams/:teamId", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5");
    assert.equal(options.method, "GET");

    return new Response(
      JSON.stringify({
        id: 5,
        name: "Nhóm Beta",
        description: "Mô tả",
        currentUserRole: "ADMIN",
        memberCount: 3,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  });

  const detail = await teamService.getTeamDetail(5);
  assert.equal(detail.id, 5);
  assert.equal(detail.name, "Nhóm Beta");
  assert.equal(detail.currentUserRole, "ADMIN");
  assert.equal(detail.memberCount, 3);
});

test("teamService.getTeamMembers calls GET /api/v1/teams/:teamId/members with pagination", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5/members?page=0&size=10");
    assert.equal(options.method, "GET");

    return new Response(
      JSON.stringify({
        content: [
          {
            id: 10,
            teamId: 5,
            userId: 1,
            username: "alice",
            email: "alice@example.com",
            role: "OWNER",
            joinedAt: new Date().toISOString(),
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  });

  const page = await teamService.getTeamMembers(5, 0, 10);
  assert.equal(page.totalElements, 1);
  assert.equal(page.content[0].username, "alice");
  assert.equal(page.content[0].role, "OWNER");
});

test("teamService.searchCandidates calls GET /api/v1/teams/:teamId/candidate-users", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5/candidate-users?query=bob");
    assert.equal(options.method, "GET");

    return new Response(
      JSON.stringify([
        {
          id: 2,
          username: "bob",
          email: "bob@example.com",
          fullname: "Bob Nguyen",
        },
      ]),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  });

  const candidates = await teamService.searchCandidates(5, "bob");
  assert.equal(candidates.length, 1);
  assert.equal(candidates[0].username, "bob");
});

test("teamService.addMember calls POST /api/v1/teams/:teamId/members", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5/members");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.userId, 2);

    return new Response(
      JSON.stringify({
        id: 20,
        teamId: 5,
        userId: 2,
        username: "bob",
        email: "bob@example.com",
        role: "MEMBER",
        joinedAt: new Date().toISOString(),
      }),
      { status: 201, headers: { "Content-Type": "application/json" } }
    );
  });

  const newMember = await teamService.addMember(5, { userId: 2 });
  assert.equal(newMember.id, 20);
  assert.equal(newMember.username, "bob");
  assert.equal(newMember.role, "MEMBER");
});

test("teamService.updateMemberRole calls PATCH /api/v1/teams/:teamId/members/:userId/role", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5/members/2/role");
    assert.equal(options.method, "PATCH");
    const body = JSON.parse(options.body);
    assert.equal(body.role, "ADMIN");

    return new Response(
      JSON.stringify({
        id: 20,
        teamId: 5,
        userId: 2,
        username: "bob",
        email: "bob@example.com",
        role: "ADMIN",
        joinedAt: new Date().toISOString(),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  });

  const updated = await teamService.updateMemberRole(5, 2, { role: "ADMIN" });
  assert.equal(updated.id, 20);
  assert.equal(updated.role, "ADMIN");
});

test("teamService.removeMember calls DELETE /api/v1/teams/:teamId/members/:userId", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "/backend/api/v1/teams/5/members/2");
    assert.equal(options.method, "DELETE");

    return new Response(null, { status: 204 });
  });

  await assert.doesNotReject(() => teamService.removeMember(5, 2));
});
