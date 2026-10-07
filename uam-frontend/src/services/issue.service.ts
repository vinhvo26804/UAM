import { apiClient } from "../lib/api-client.ts";
import type {
  Issue,
  IssueSummary,
  IssueType,
  IssueStatus,
  IssuePriority,
  IssueHistory,
  IssueSprintHistory,
  PageResponse,
} from "../types/issue.ts";

export interface CreateIssueDto {
  spaceId: number;
  type: IssueType;
  summary: string;
  description?: string;
  reporterId?: number;
  assigneeId?: number;
  priority?: IssuePriority;
  epicId?: number;
  parentIssueId?: number;
  sprintId?: number;
  estimateHours?: number;
  dueDate?: string;
  labels?: string[];
}

export interface UpdateIssueDto {
  version: number;
  summary?: string;
  description?: string;
  reporterId?: number;
  assigneeId?: number | null;
  priority?: IssuePriority;
  epicId?: number | null;
  sprintId?: number | null;
  estimateHours?: number | null;
  dueDate?: string | null;
  labels?: string[];
}

export interface SearchIssueParams {
  spaceId: number;
  q?: string;
  type?: IssueType;
  status?: IssueStatus;
  assigneeId?: number;
  epicId?: number;
  parentIssueId?: number;
  sprintId?: number;
  backlog?: boolean;
  page?: number;
  size?: number;
}

export const issueService = {
  async createIssue(dto: CreateIssueDto, idempotencyKey?: string): Promise<Issue> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers["Idempotency-Key"] = idempotencyKey;
    }

    return await apiClient<Issue>("/api/v1/issues", {
      method: "POST",
      headers,
      body: dto,
    });
  },

  async searchIssues(params: SearchIssueParams): Promise<PageResponse<IssueSummary>> {
    const query = new URLSearchParams();
    query.set("spaceId", params.spaceId.toString());

    if (params.q) query.set("q", params.q);
    if (params.type) query.set("type", params.type);
    if (params.status) query.set("status", params.status);
    if (params.assigneeId !== undefined) query.set("assigneeId", params.assigneeId.toString());
    if (params.epicId !== undefined) query.set("epicId", params.epicId.toString());
    if (params.parentIssueId !== undefined) query.set("parentIssueId", params.parentIssueId.toString());
    if (params.sprintId !== undefined) query.set("sprintId", params.sprintId.toString());
    if (params.backlog !== undefined) query.set("backlog", params.backlog.toString());
    query.set("page", (params.page ?? 0).toString());
    query.set("size", (params.size ?? 20).toString());

    return await apiClient<PageResponse<IssueSummary>>(`/api/v1/issues?${query.toString()}`);
  },

  async getIssueDetail(id: number): Promise<Issue> {
    return await apiClient<Issue>(`/api/v1/issues/${id}`);
  },

  async updateIssue(id: number, dto: UpdateIssueDto): Promise<Issue> {
    const payload = {
      version: dto.version,
      summary: dto.summary !== undefined ? dto.summary : null,
      description: dto.description !== undefined ? dto.description : null,
      reporterId: dto.reporterId !== undefined ? dto.reporterId : null,
      assigneeId: dto.assigneeId !== undefined ? dto.assigneeId : null,
      priority: dto.priority !== undefined ? dto.priority : null,
      epicId: dto.epicId !== undefined ? dto.epicId : null,
      sprintId: dto.sprintId !== undefined ? dto.sprintId : null,
      estimateHours: dto.estimateHours !== undefined ? dto.estimateHours : null,
      dueDate: dto.dueDate !== undefined ? dto.dueDate : null,
      labels: dto.labels !== undefined ? dto.labels : null,
      summaryPresent: dto.summary !== undefined,
      descriptionPresent: dto.description !== undefined,
      reporterIdPresent: dto.reporterId !== undefined,
      assigneeIdPresent: dto.assigneeId !== undefined,
      priorityPresent: dto.priority !== undefined,
      epicIdPresent: dto.epicId !== undefined,
      sprintIdPresent: dto.sprintId !== undefined,
      estimateHoursPresent: dto.estimateHours !== undefined,
      dueDatePresent: dto.dueDate !== undefined,
      labelsPresent: dto.labels !== undefined,
    };

    return await apiClient<Issue>(`/api/v1/issues/${id}`, {
      method: "PATCH",
      body: payload,
    });
  },

  async changeStatus(id: number, targetStatus: IssueStatus, version: number): Promise<Issue> {
    return await apiClient<Issue>(`/api/v1/issues/${id}/status`, {
      method: "POST",
      body: { targetStatus, version },
    });
  },

  async deleteIssue(id: number, version?: number): Promise<void> {
    let url = `/api/v1/issues/${id}`;
    if (version !== undefined) {
      url += `?version=${version}`;
    }
    await apiClient<void>(url, {
      method: "DELETE",
    });
  },

  async getIssueAuditHistory(id: number, page = 0, size = 20): Promise<PageResponse<IssueHistory>> {
    return await apiClient<PageResponse<IssueHistory>>(`/api/v1/issues/${id}/history?page=${page}&size=${size}`);
  },

  async getIssueSprintHistory(id: number, page = 0, size = 20): Promise<PageResponse<IssueSprintHistory>> {
    return await apiClient<PageResponse<IssueSprintHistory>>(`/api/v1/issues/${id}/sprint-history?page=${page}&size=${size}`);
  },
};
