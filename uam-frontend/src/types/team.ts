export type TeamRole = "OWNER" | "ADMIN" | "MEMBER";

export interface Team {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  currentUserRole: TeamRole;
  memberCount: number;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface TeamMember {
  id: number;
  teamId: number;
  userId: number;
  username: string;
  email: string;
  fullname?: string;
  role: TeamRole;
  joinedAt: string;
}

export interface AddTeamMemberRequest {
  userId: number;
}

export interface UpdateTeamMemberRoleRequest {
  role: TeamRole;
}

export interface CandidateUser {
  id: number;
  username: string;
  email: string;
  fullname?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
