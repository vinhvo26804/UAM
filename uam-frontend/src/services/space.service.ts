import { apiClient } from "../lib/api-client.ts";
import type { Space, SpaceMember, SpaceRole, IssueType, PageResponse } from "../types/issue.ts";

export interface CreateSpaceDto {
  key: string;
  name: string;
  allowedIssueTypes?: IssueType[];
}

export interface AddSpaceMemberDto {
  userId: number;
  role: SpaceRole;
}

export interface UpdateSpaceMemberRoleDto {
  role: SpaceRole;
  expectedRole?: SpaceRole;
}

export const spaceService = {
  async createSpace(dto: CreateSpaceDto): Promise<Space> {
    return await apiClient<Space>("/api/v1/spaces", {
      method: "POST",
      body: dto,
    });
  },

  async getMySpaces(page = 0, size = 20): Promise<PageResponse<Space>> {
    return await apiClient<PageResponse<Space>>(`/api/v1/spaces?page=${page}&size=${size}`);
  },

  async getSpaceDetail(id: number): Promise<Space> {
    return await apiClient<Space>(`/api/v1/spaces/${id}`);
  },

  async getMembers(spaceId: number, query?: string, page = 0, size = 20): Promise<PageResponse<SpaceMember>> {
    let url = `/api/v1/spaces/${spaceId}/members?page=${page}&size=${size}`;
    if (query) {
      url += `&query=${encodeURIComponent(query)}`;
    }
    return await apiClient<PageResponse<SpaceMember>>(url);
  },

  async searchCandidates(spaceId: number, query: string): Promise<Array<{ id: number; username: string; fullname: string }>> {
    return await apiClient<Array<{ id: number; username: string; fullname: string }>>(
      `/api/v1/spaces/${spaceId}/candidate-users?query=${encodeURIComponent(query)}`
    );
  },

  async addMember(spaceId: number, dto: AddSpaceMemberDto): Promise<SpaceMember> {
    return await apiClient<SpaceMember>(`/api/v1/spaces/${spaceId}/members`, {
      method: "POST",
      body: dto,
    });
  },

  async updateMemberRole(spaceId: number, userId: number, dto: UpdateSpaceMemberRoleDto): Promise<SpaceMember> {
    return await apiClient<SpaceMember>(`/api/v1/spaces/${spaceId}/members/${userId}/role`, {
      method: "PATCH",
      body: dto,
    });
  },

  async removeMember(spaceId: number, userId: number, expectedRole?: SpaceRole): Promise<void> {
    let url = `/api/v1/spaces/${spaceId}/members/${userId}`;
    if (expectedRole) {
      url += `?expectedRole=${expectedRole}`;
    }
    await apiClient<void>(url, {
      method: "DELETE",
    });
  },
};
