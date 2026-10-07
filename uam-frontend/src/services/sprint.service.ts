import { apiClient } from "../lib/api-client.ts";
import type { Sprint, SprintStatus, SprintCompletionPreview, CompleteSprintResult, PageResponse } from "../types/issue.ts";

export interface CreateSprintDto {
  spaceId: number;
  name: string;
  goal?: string;
  plannedStartAt?: string;
  plannedEndAt?: string;
}

export interface CompleteSprintDto {
  version: number;
  spaceRevision: number;
  targetSprintId?: number;
  targetVersion?: number;
  newSprint?: {
    name: string;
    goal?: string;
    plannedStartAt?: string;
    plannedEndAt?: string;
  };
}

export const sprintService = {
  async createSprint(dto: CreateSprintDto): Promise<Sprint> {
    return await apiClient<Sprint>("/api/v1/sprints", {
      method: "POST",
      body: dto,
    });
  },

  async getSprints(spaceId: number, status?: SprintStatus, page = 0, size = 20): Promise<PageResponse<Sprint>> {
    let url = `/api/v1/sprints?spaceId=${spaceId}&page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    return await apiClient<PageResponse<Sprint>>(url);
  },

  async getSprintDetail(id: number): Promise<Sprint> {
    return await apiClient<Sprint>(`/api/v1/sprints/${id}`);
  },

  async startSprint(id: number, version: number): Promise<Sprint> {
    return await apiClient<Sprint>(`/api/v1/sprints/${id}/start`, {
      method: "POST",
      body: { version },
    });
  },

  async previewComplete(id: number, targetSprintId?: number, page = 0, size = 20): Promise<SprintCompletionPreview> {
    const payload = targetSprintId ? { targetSprintId } : {};
    return await apiClient<SprintCompletionPreview>(
      `/api/v1/sprints/${id}/complete-preview?page=${page}&size=${size}`,
      {
        method: "POST",
        body: payload,
      }
    );
  },

  async completeSprint(id: number, dto: CompleteSprintDto, idempotencyKey?: string): Promise<CompleteSprintResult> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers["Idempotency-Key"] = idempotencyKey;
    }
    return await apiClient<CompleteSprintResult>(`/api/v1/sprints/${id}/complete`, {
      method: "POST",
      body: dto,
      headers,
    });
  },
};
