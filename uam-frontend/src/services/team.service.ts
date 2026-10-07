import { apiClient } from "../lib/api-client.ts";
import type {
  Team,
  CreateTeamRequest,
  TeamMember,
  AddTeamMemberRequest,
  UpdateTeamMemberRoleRequest,
  CandidateUser,
  PageResponse,
} from "../types/team.ts";

export const teamService = {
  /**
   * Tạo nhóm làm việc mới (POST /api/v1/teams)
   */
  async createTeam(data: CreateTeamRequest): Promise<Team> {
    return await apiClient<Team>("/api/v1/teams", {
      method: "POST",
      body: {
        name: data.name.trim(),
        description: data.description?.trim() || undefined,
      },
    });
  },

  /**
   * Lấy danh sách nhóm của user hiện tại (GET /api/v1/teams)
   */
  async getMyTeams(): Promise<Team[]> {
    return await apiClient<Team[]>("/api/v1/teams", {
      method: "GET",
    });
  },

  /**
   * Lấy chi tiết nhóm (GET /api/v1/teams/{teamId})
   */
  async getTeamDetail(teamId: number): Promise<Team> {
    return await apiClient<Team>(`/api/v1/teams/${teamId}`, {
      method: "GET",
    });
  },

  /**
   * Lấy danh sách thành viên phân trang (GET /api/v1/teams/{teamId}/members)
   */
  async getTeamMembers(
    teamId: number,
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<TeamMember>> {
    return await apiClient<PageResponse<TeamMember>>(
      `/api/v1/teams/${teamId}/members?page=${page}&size=${size}`,
      {
        method: "GET",
      }
    );
  },

  /**
   * Tìm kiếm user khả dụng để thêm vào nhóm (GET /api/v1/teams/{teamId}/candidate-users)
   */
  async searchCandidates(teamId: number, query: string): Promise<CandidateUser[]> {
    return await apiClient<CandidateUser[]>(
      `/api/v1/teams/${teamId}/candidate-users?query=${encodeURIComponent(query.trim())}`,
      {
        method: "GET",
      }
    );
  },

  /**
   * Thêm thành viên vào nhóm (POST /api/v1/teams/{teamId}/members)
   */
  async addMember(teamId: number, data: AddTeamMemberRequest): Promise<TeamMember> {
    return await apiClient<TeamMember>(`/api/v1/teams/${teamId}/members`, {
      method: "POST",
      body: data,
    });
  },

  /**
   * Đổi vai trò thành viên (PATCH /api/v1/teams/{teamId}/members/{userId}/role)
   */
  async updateMemberRole(
    teamId: number,
    userId: number,
    data: UpdateTeamMemberRoleRequest
  ): Promise<TeamMember> {
    return await apiClient<TeamMember>(`/api/v1/teams/${teamId}/members/${userId}/role`, {
      method: "PATCH",
      body: data,
    });
  },

  /**
   * Xóa thành viên hoặc rời nhóm (DELETE /api/v1/teams/{teamId}/members/{userId})
   */
  async removeMember(teamId: number, userId: number): Promise<void> {
    return await apiClient<void>(`/api/v1/teams/${teamId}/members/${userId}`, {
      method: "DELETE",
    });
  },
};
