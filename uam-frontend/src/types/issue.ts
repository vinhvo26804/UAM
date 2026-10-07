export type IssueType = 'EPIC' | 'STORY' | 'TASK' | 'BUG' | 'SUBTASK';
export type IssueStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type IssuePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type SpaceRole = 'ADMIN' | 'MANAGER' | 'MEMBER' | 'VIEWER';
export type SprintStatus = 'FUTURE' | 'ACTIVE' | 'CLOSED';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface Space {
  id: number;
  key: string;
  name: string;
  archived: boolean;
  workRevision: number;
  allowedIssueTypes: IssueType[];
  currentUserRole?: SpaceRole;
  capabilities: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SpaceMember {
  id: number;
  userId: number;
  username: string;
  fullname: string;
  role: SpaceRole;
  active: boolean;
  eligibleForAssign: boolean;
  joinedAt: string;
}

export interface Sprint {
  id: number;
  spaceId: number;
  name: string;
  goal?: string;
  status: SprintStatus;
  position: number;
  plannedStartAt?: string;
  plannedEndAt?: string;
  startedAt?: string;
  completedAt?: string;
  version: number;
  allowedActions: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserReference {
  id: number;
  username: string;
  displayName: string;
}

export interface Issue {
  id: number;
  key: string;
  spaceId: number;
  type: IssueType;
  summary: string;
  description?: string;
  status: IssueStatus;
  statusCategory: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: IssuePriority;
  reporter: UserReference;
  assignee?: UserReference;
  assigneeEligible?: boolean;
  epicId?: number;
  epicKey?: string;
  epicSummary?: string;
  inheritedEpicId?: number;
  parentIssueId?: number;
  parentIssueKey?: string;
  parentIssueSummary?: string;
  sprintId?: number;
  sprintName?: string;
  estimateHours?: number;
  dueDate?: string;
  labels: string[];
  childCount: number;
  version: number;
  createdBy: number;
  updatedBy: number;
  createdAt: string;
  updatedAt: string;
  allowedActions: string[];
}

export interface IssueSummary {
  id: number;
  key: string;
  spaceId: number;
  type: IssueType;
  summary: string;
  status: IssueStatus;
  statusCategory: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: IssuePriority;
  assignee?: UserReference;
  assigneeEligible?: boolean;
  epicId?: number;
  epicKey?: string;
  inheritedEpicId?: number;
  parentIssueId?: number;
  parentIssueKey?: string;
  sprintId?: number;
  sprintName?: string;
  estimateHours?: number;
  dueDate?: string;
  version: number;
  createdAt: string;
  allowedActions: string[];
}

export interface IssueHistory {
  id: number;
  issueId: number;
  actorId: number;
  actorUsername: string;
  actorDisplayName: string;
  action: string;
  changes: string;
  createdAt: string;
}

export interface IssueSprintHistory {
  id: number;
  issueId: number;
  sprintId: number;
  sprintName: string;
  enteredAt: string;
  leftAt?: string;
  enteredById: number;
  enteredByDisplayName: string;
  leftById?: number;
  leftByDisplayName?: string;
  enterReason: string;
  leaveReason?: string;
}

export interface SprintCompletionPreview {
  sourceSprintId: number;
  sourceVersion: number;
  spaceRevision: number;
  targetSprintId?: number;
  targetVersion?: number;
  suggestedTargetSprintId?: number;
  moveRootCount: number;
  moveIssueCount: number;
  includedDoneSubtaskCount: number;
  stayCount: number;
  canComplete: boolean;
  blockers: string[];
  items: {
    content: Array<{
      id: number;
      key: string;
      type: IssueType;
      status: IssueStatus;
      parentIssueId?: number;
      reason: string;
    }>;
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };
}

export interface CompleteSprintResult {
  sourceSprintId: number;
  sourceStatus: string;
  sourceVersion: number;
  targetSprintId?: number;
  movedIssueCount: number;
  retainedIssueCount: number;
  spaceRevision: number;
}
