"use client";

import { useEffect, useState, use, useCallback } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  IssueSummary,
  IssueType,
  IssueStatus,
  IssuePriority,
  Space,
  Sprint,
  SpaceMember,
} from "@/types/issue";
import { issueService } from "@/services/issue.service";
import { spaceService } from "@/services/space.service";
import { sprintService } from "@/services/sprint.service";
import { extractErrorMessage } from "@/lib/api-client";
import {
  IssueTypeIcon,
  PriorityIcon,
  StatusLozenge,
  SprintIcon,
  BacklogIcon,
  JiraAvatar,
} from "@/components/jira/JiraIcons";
import CreateIssueModal from "@/components/issues/CreateIssueModal";

interface PageProps {
  params: Promise<{ spaceId: string }>;
}

export default function SpaceIssuesPage({ params }: PageProps) {
  const resolvedParams = use(params);
  const spaceId = Number(resolvedParams.spaceId);
  const router = useRouter();
  const searchParams = useSearchParams();
  const sprintIdParam = searchParams.get("sprintId");

  const [space, setSpace] = useState<Space | null>(null);
  const [issues, setIssues] = useState<IssueSummary[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [members, setMembers] = useState<SpaceMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Group accordion state (group key -> isCollapsed)
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

  // Inline updating indicator
  const [updatingField, setUpdatingField] = useState<{ issueId: number; field: string } | null>(null);

  // Filters
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState<IssueType | "">("");
  const [statusFilter, setStatusFilter] = useState<IssueStatus | "">("");
  const [internalSprintFilter, setInternalSprintFilter] = useState<string>(sprintIdParam || "");

  const sprintFilter = sprintIdParam !== null ? sprintIdParam : internalSprintFilter;

  // Modal Create
  const [createModalConfig, setCreateModalConfig] = useState<{
    isOpen: boolean;
    defaultSprintId?: number | null;
  }>({
    isOpen: false,
    defaultSprintId: null,
  });

  const toggleGroupCollapse = (groupKey: string) => {
    setCollapsedGroups((prev) => ({
      ...prev,
      [groupKey]: !prev[groupKey],
    }));
  };

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const isBacklog = sprintFilter === "backlog";
      const selectedSprintId = sprintFilter && sprintFilter !== "backlog" ? Number(sprintFilter) : undefined;

      const [spaceData, issuesData, sprintsData, membersData] = await Promise.all([
        spaceService.getSpaceDetail(spaceId),
        issueService.searchIssues({
          spaceId,
          q: searchTerm.trim() || undefined,
          type: typeFilter || undefined,
          status: statusFilter || undefined,
          sprintId: selectedSprintId,
          backlog: isBacklog ? true : undefined,
          size: 100,
        }),
        sprintService.getSprints(spaceId, undefined, 0, 50),
        spaceService.getMembers(spaceId, undefined, 0, 100),
      ]);
      setSpace(spaceData);
      setIssues(issuesData.content);
      setSprints(sprintsData.content);
      setMembers(membersData.content.filter((m) => m.eligibleForAssign));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [spaceId, searchTerm, typeFilter, statusFilter, sprintFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Inline Editing: Priority
  const handlePriorityChange = async (issue: IssueSummary, newPriority: IssuePriority) => {
    if (issue.priority === newPriority) return;
    setUpdatingField({ issueId: issue.id, field: "priority" });
    setActionError(null);
    try {
      const updated = await issueService.updateIssue(issue.id, {
        version: issue.version,
        priority: newPriority,
      });
      setIssues((prev) =>
        prev.map((item) =>
          item.id === issue.id
            ? {
                ...item,
                version: updated.version,
                priority: updated.priority,
              }
            : item
        )
      );
      setActionSuccess(`Đã cập nhật độ ưu tiên của [${issue.key}] thành ${newPriority}`);
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(`Lỗi cập nhật độ ưu tiên [${issue.key}]: ${extractErrorMessage(err)}`);
    } finally {
      setUpdatingField(null);
    }
  };

  // Inline Editing: Assignee
  const handleAssigneeChange = async (issue: IssueSummary, newAssigneeId: number | null) => {
    if ((issue.assignee?.id || null) === newAssigneeId) return;
    setUpdatingField({ issueId: issue.id, field: "assignee" });
    setActionError(null);
    try {
      const updated = await issueService.updateIssue(issue.id, {
        version: issue.version,
        assigneeId: newAssigneeId,
      });
      setIssues((prev) =>
        prev.map((item) =>
          item.id === issue.id
            ? {
                ...item,
                version: updated.version,
                assignee: updated.assignee,
              }
            : item
        )
      );
      setActionSuccess(
        newAssigneeId
          ? `Đã giao việc [${issue.key}] cho ${updated.assignee?.displayName}`
          : `Đã hủy giao việc cho [${issue.key}]`
      );
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(`Lỗi cập nhật người nhận [${issue.key}]: ${extractErrorMessage(err)}`);
    } finally {
      setUpdatingField(null);
    }
  };

  // Inline Editing: Sprint
  const handleSprintChange = async (issue: IssueSummary, newSprintId: number | null) => {
    if ((issue.sprintId || null) === newSprintId) return;
    setUpdatingField({ issueId: issue.id, field: "sprint" });
    setActionError(null);
    try {
      const updated = await issueService.updateIssue(issue.id, {
        version: issue.version,
        sprintId: newSprintId,
      });
      setIssues((prev) =>
        prev.map((item) =>
          item.id === issue.id
            ? {
                ...item,
                version: updated.version,
                sprintId: updated.sprintId,
                sprintName: updated.sprintName,
              }
            : item
        )
      );
      setActionSuccess(
        newSprintId
          ? `Đã chuyển [${issue.key}] vào sprint ${updated.sprintName}`
          : `Đã đưa [${issue.key}] về Backlog`
      );
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(`Lỗi chuyển Sprint [${issue.key}]: ${extractErrorMessage(err)}`);
    } finally {
      setUpdatingField(null);
    }
  };

  // Row click handler: opens issue detail unless clicking interactive elements
  const handleRowClick = (e: React.MouseEvent, issueId: number) => {
    const target = e.target as HTMLElement;
    if (
      target.tagName === "SELECT" ||
      target.tagName === "OPTION" ||
      target.tagName === "BUTTON" ||
      target.tagName === "INPUT" ||
      target.closest("select") ||
      target.closest("button") ||
      target.closest("a")
    ) {
      return;
    }
    router.push(`/spaces/${spaceId}/issues/${issueId}`);
  };

  // Grouping issues by Sprint and Backlog
  const backlogIssues = issues.filter((i) => !i.sprintId);

  // Determine which sprints to display
  const displayedSprints = sprintFilter && sprintFilter !== "backlog"
    ? sprints.filter((s) => String(s.id) === sprintFilter)
    : sprints;

  const isUserCanEdit = space?.currentUserRole !== "VIEWER";
  const canManageSprint = space?.currentUserRole === "ADMIN" || space?.currentUserRole === "MANAGER";

  return (
    <div className="max-w-6xl mx-auto space-y-5 animate-jira-fade">
      {/* Header & Breadcrumbs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-[#091E4224] gap-4">
        <div>
          <div className="flex items-center gap-1.5 text-xs text-[#626F86] mb-1">
            <Link href="/spaces" className="hover:text-[#0C66E4] transition-colors font-medium">
              Không gian
            </Link>
            <span>/</span>
            <span className="font-semibold text-[#172B4D]">{space?.key || "..."}</span>
            <span>/</span>
            <span>Danh sách công việc</span>
          </div>
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded bg-[#0C66E4] text-white flex items-center justify-center font-bold text-xs shadow-xs">
              {space?.key?.slice(0, 2) || "SP"}
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] tracking-tight">
              {space ? space.name : "Đang tải Không gian..."}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <Link
            href={`/spaces/${spaceId}/sprints`}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-[#091E4224] bg-white hover:bg-[#091E420F] text-[#44546F] text-xs font-semibold rounded shadow-2xs transition-colors jira-interactive-btn"
          >
            <SprintIcon className="w-3.5 h-3.5 text-[#0C66E4]" />
            <span>Quản lý Sprints</span>
          </Link>
          <button
            type="button"
            onClick={() => setCreateModalConfig({ isOpen: true, defaultSprintId: null })}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded transition-colors cursor-pointer shadow-xs jira-interactive-btn"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
            </svg>
            <span>Tạo công việc</span>
          </button>
        </div>
      </div>

      {/* Action Alerts / Notifications */}
      {actionError && (
        <div className="p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs flex items-center justify-between shadow-xs animate-jira-slide-down">
          <div className="flex items-center gap-2">
            <svg className="w-4 h-4 text-[#DE350B] shrink-0" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <span>{actionError}</span>
          </div>
          <button type="button" onClick={() => setActionError(null)} className="font-bold text-sm px-1 hover:text-[#BF2600]">✕</button>
        </div>
      )}

      {actionSuccess && (
        <div className="p-3 bg-[#E3FCEF] border border-[#57D9A3] text-[#006644] rounded-md text-xs flex items-center justify-between shadow-xs animate-jira-slide-down">
          <div className="flex items-center gap-2">
            <svg className="w-4 h-4 text-[#006644] shrink-0" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
            <span>{actionSuccess}</span>
          </div>
          <button type="button" onClick={() => setActionSuccess(null)} className="font-bold text-sm px-1 hover:text-[#00472e]">✕</button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="bg-white p-3 sm:p-4 rounded-lg border border-[#091E4224] flex flex-wrap items-center gap-2.5 shadow-2xs">
        {/* Search Input */}
        <div className="relative flex-1 min-w-[220px]">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#626F86]">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input
            type="text"
            placeholder="Tìm theo tiêu đề, mô tả, từ khóa..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-8 py-1.5 text-xs rounded border border-[#091E4224] focus:outline-hidden focus:border-[#0C66E4] bg-[#FAFBFC] focus:bg-white transition-colors"
          />
          {searchTerm && (
            <button
              type="button"
              onClick={() => setSearchTerm("")}
              className="absolute inset-y-0 right-0 pr-2.5 flex items-center text-[#626F86] hover:text-[#172B4D]"
            >
              ✕
            </button>
          )}
        </div>

        {/* Filter by Type */}
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value as IssueType | "")}
          className="text-xs px-2.5 py-1.5 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer"
        >
          <option value="">Tất cả loại việc</option>
          <option value="EPIC">Epic</option>
          <option value="STORY">Story</option>
          <option value="TASK">Task</option>
          <option value="BUG">Bug</option>
          <option value="SUBTASK">Subtask</option>
        </select>

        {/* Filter by Status */}
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as IssueStatus | "")}
          className="text-xs px-2.5 py-1.5 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="TODO">Cần làm (TODO)</option>
          <option value="IN_PROGRESS">Đang làm (IN_PROGRESS)</option>
          <option value="DONE">Hoàn thành (DONE)</option>
          <option value="CANCELLED">Đã hủy (CANCELLED)</option>
        </select>

        {/* Filter by Sprint */}
        <select
          value={sprintFilter}
          onChange={(e) => {
            const val = e.target.value;
            setInternalSprintFilter(val);
            if (val) {
              router.push(`/spaces/${spaceId}/issues?sprintId=${val}`);
            } else {
              router.push(`/spaces/${spaceId}/issues`);
            }
          }}
          className="text-xs px-2.5 py-1.5 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer"
        >
          <option value="">Tất cả chu kỳ Sprint</option>
          <option value="backlog">Chỉ Backlog</option>
          {sprints.map((sp) => (
            <option key={sp.id} value={sp.id}>
              {sp.name} ({sp.status})
            </option>
          ))}
        </select>

        {(searchTerm || typeFilter || statusFilter || sprintFilter) && (
          <button
            type="button"
            onClick={() => {
              setSearchTerm("");
              setTypeFilter("");
              setStatusFilter("");
              setInternalSprintFilter("");
              router.push(`/spaces/${spaceId}/issues`);
            }}
            className="text-xs text-[#0C66E4] hover:underline px-2 py-1 font-medium cursor-pointer"
          >
            Xóa bộ lọc
          </button>
        )}

        <div className="ml-auto text-[11px] font-medium text-[#626F86]">
          {issues.length} công việc
        </div>
      </div>

      {/* Main Content: Grouped by Sprint and Backlog */}
      {loading ? (
        <IssueListSkeleton />
      ) : error ? (
        <div className="p-4 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-lg text-xs">
          {error}
        </div>
      ) : (
        <div className="space-y-5">
          {/* 1. SPRINT GROUPS */}
          {sprintFilter !== "backlog" &&
            displayedSprints.map((sprint) => {
              const sprintIssues = issues.filter((i) => i.sprintId === sprint.id);
              const isCollapsed = collapsedGroups[`sprint-${sprint.id}`] ?? false;

              return (
                <div
                  key={sprint.id}
                  className="bg-white rounded-lg border border-[#091E4224] overflow-hidden shadow-2xs transition-shadow hover:shadow-xs"
                >
                  {/* Sprint Group Header */}
                  <div className="p-3 bg-[#FAFBFC] border-b border-[#091E4214] flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => toggleGroupCollapse(`sprint-${sprint.id}`)}
                        className="text-[#626F86] hover:text-[#172B4D] p-1 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
                        title={isCollapsed ? "Mở rộng Sprint" : "Thu gọn Sprint"}
                      >
                        <svg
                          className={`w-3.5 h-3.5 transform transition-transform duration-200 ${isCollapsed ? "-rotate-90" : "rotate-0"}`}
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>

                      <div className="flex items-center gap-2 flex-wrap">
                        <SprintIcon className="w-4 h-4 text-[#0C66E4]" />
                        <span className="font-bold text-sm text-[#172B4D]">{sprint.name}</span>
                        <span
                          className={`px-2 py-0.5 text-[10px] font-bold rounded uppercase tracking-wider ${
                            sprint.status === "ACTIVE"
                              ? "bg-[#E3FCEF] text-[#006644] border border-[#ABF5D1]"
                              : sprint.status === "FUTURE"
                              ? "bg-[#DEEBFF] text-[#0747A6] border border-[#B3D4FF]"
                              : "bg-[#DFE1E6] text-[#42526E] border border-[#C1C7D0]"
                          }`}
                        >
                          {sprint.status}
                        </span>
                        <span className="text-xs text-[#626F86] font-medium">
                          ({sprintIssues.length} công việc)
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      {sprint.plannedStartAt && sprint.plannedEndAt && (
                        <span className="text-[11px] text-[#626F86] font-mono">
                          {sprint.plannedStartAt.substring(0, 10)} &rarr; {sprint.plannedEndAt.substring(0, 10)}
                        </span>
                      )}
                      <Link
                        href={`/spaces/${spaceId}/sprints`}
                        className="text-xs text-[#0C66E4] hover:underline font-semibold"
                      >
                        Chi tiết Sprint &rarr;
                      </Link>
                    </div>
                  </div>

                  {/* Sprint Issues List */}
                  {!isCollapsed && (
                    <div>
                      {sprintIssues.length === 0 ? (
                        <div className="py-7 text-center text-xs text-[#626F86] bg-[#FAFBFC]/40 italic">
                          Sprint này chưa có công việc nào.
                        </div>
                      ) : (
                        <div className="divide-y divide-[#091E4214]">
                          {sprintIssues.map((issue) => (
                            <IssueRowItem
                              key={issue.id}
                              issue={issue}
                              spaceId={spaceId}
                              sprints={sprints}
                              members={members}
                              canEdit={isUserCanEdit}
                              canManageSprint={canManageSprint}
                              updatingField={updatingField}
                              onRowClick={handleRowClick}
                              onPriorityChange={handlePriorityChange}
                              onAssigneeChange={handleAssigneeChange}
                              onSprintChange={handleSprintChange}
                            />
                          ))}
                        </div>
                      )}

                      {/* Quick Create in Sprint Button */}
                      <div className="p-2 bg-[#FAFBFC] border-t border-[#091E4214]">
                        <button
                          type="button"
                          onClick={() => setCreateModalConfig({ isOpen: true, defaultSprintId: sprint.id })}
                          className="inline-flex items-center gap-1.5 text-xs text-[#0C66E4] font-medium hover:bg-[#0C66E40F] px-2.5 py-1 rounded transition-colors cursor-pointer jira-interactive-btn"
                        >
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                          </svg>
                          <span>+ Tạo công việc trong {sprint.name}</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}

          {/* 2. BACKLOG GROUP */}
          {(!sprintFilter || sprintFilter === "backlog") && (
            <div className="bg-white rounded-lg border border-[#091E4224] overflow-hidden shadow-2xs transition-shadow hover:shadow-xs">
              {/* Backlog Group Header */}
              <div className="p-3 bg-[#FAFBFC] border-b border-[#091E4214] flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => toggleGroupCollapse("backlog")}
                    className="text-[#626F86] hover:text-[#172B4D] p-1 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
                    title={collapsedGroups["backlog"] ? "Mở rộng Backlog" : "Thu gọn Backlog"}
                  >
                    <svg
                      className={`w-3.5 h-3.5 transform transition-transform duration-200 ${collapsedGroups["backlog"] ? "-rotate-90" : "rotate-0"}`}
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>

                  <div className="flex items-center gap-2">
                    <BacklogIcon className="w-4 h-4 text-[#626F86]" />
                    <span className="font-bold text-sm text-[#172B4D]">Backlog</span>
                    <span className="px-2 py-0.5 bg-[#091E420F] text-[#44546F] text-xs font-semibold rounded-full">
                      {backlogIssues.length} công việc
                    </span>
                  </div>
                </div>

                <span className="text-xs text-[#626F86] hidden sm:inline">
                  Các công việc chưa được phân vào chu kỳ Sprint
                </span>
              </div>

              {/* Backlog Issues List */}
              {!collapsedGroups["backlog"] && (
                <div>
                  {backlogIssues.length === 0 ? (
                    <div className="py-7 text-center text-xs text-[#626F86] bg-[#FAFBFC]/40 italic">
                      Backlog trống. Không có công việc nào chưa gán sprint.
                    </div>
                  ) : (
                    <div className="divide-y divide-[#091E4214]">
                      {backlogIssues.map((issue) => (
                        <IssueRowItem
                          key={issue.id}
                          issue={issue}
                          spaceId={spaceId}
                          sprints={sprints}
                          members={members}
                          canEdit={isUserCanEdit}
                          canManageSprint={canManageSprint}
                          updatingField={updatingField}
                          onRowClick={handleRowClick}
                          onPriorityChange={handlePriorityChange}
                          onAssigneeChange={handleAssigneeChange}
                          onSprintChange={handleSprintChange}
                        />
                      ))}
                    </div>
                  )}

                  {/* Quick Create in Backlog Button */}
                  <div className="p-2 bg-[#FAFBFC] border-t border-[#091E4214]">
                    <button
                      type="button"
                      onClick={() => setCreateModalConfig({ isOpen: true, defaultSprintId: null })}
                      className="inline-flex items-center gap-1.5 text-xs text-[#0C66E4] font-medium hover:bg-[#0C66E40F] px-2.5 py-1 rounded transition-colors cursor-pointer jira-interactive-btn"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                      </svg>
                      <span>+ Tạo công việc trong Backlog</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Global & Quick Create Modal */}
      {createModalConfig.isOpen && (
        <CreateIssueModal
          isOpen={createModalConfig.isOpen}
          onClose={() => setCreateModalConfig({ isOpen: false })}
          defaultSpaceId={spaceId}
          defaultSprintId={createModalConfig.defaultSprintId}
          onSuccess={() => {
            fetchData();
          }}
        />
      )}
    </div>
  );
}

// Sub-component for an Issue row with full inline-editing & Jira styling
interface IssueRowItemProps {
  issue: IssueSummary;
  spaceId: number;
  sprints: Sprint[];
  members: SpaceMember[];
  canEdit: boolean;
  canManageSprint: boolean;
  updatingField: { issueId: number; field: string } | null;
  onRowClick: (e: React.MouseEvent, issueId: number) => void;
  onPriorityChange: (issue: IssueSummary, newPriority: IssuePriority) => void;
  onAssigneeChange: (issue: IssueSummary, newAssigneeId: number | null) => void;
  onSprintChange: (issue: IssueSummary, newSprintId: number | null) => void;
}

function IssueRowItem({
  issue,
  spaceId,
  sprints,
  members,
  canEdit,
  canManageSprint,
  updatingField,
  onRowClick,
  onPriorityChange,
  onAssigneeChange,
  onSprintChange,
}: IssueRowItemProps) {
  const isUpdatingPriority = updatingField?.issueId === issue.id && updatingField.field === "priority";
  const isUpdatingAssignee = updatingField?.issueId === issue.id && updatingField.field === "assignee";
  const isUpdatingSprint = updatingField?.issueId === issue.id && updatingField.field === "sprint";

  return (
    <div
      onClick={(e) => onRowClick(e, issue.id)}
      className="p-2.5 sm:p-3 flex flex-col md:flex-row md:items-center justify-between gap-2.5 hover:bg-[#FAFBFC] border-l-2 border-transparent hover:border-l-[#0C66E4] jira-interactive-row cursor-pointer group"
    >
      {/* Left: Type Icon, Key, Summary, Parent chips */}
      <div className="flex items-center gap-2.5 min-w-0 flex-1">
        <div className="shrink-0">
          <IssueTypeIcon type={issue.type} size={16} />
        </div>

        <Link
          href={`/spaces/${spaceId}/issues/${issue.id}`}
          onClick={(e) => e.stopPropagation()}
          className="font-mono text-xs font-semibold text-[#0C66E4] hover:underline shrink-0"
        >
          {issue.key}
        </Link>

        <div className="min-w-0 flex-1 flex items-center gap-2 flex-wrap">
          <Link
            href={`/spaces/${spaceId}/issues/${issue.id}`}
            onClick={(e) => e.stopPropagation()}
            className="text-xs sm:text-sm font-medium text-[#172B4D] hover:text-[#0C66E4] hover:underline truncate"
            title={issue.summary}
          >
            {issue.summary}
          </Link>

          {issue.epicKey && (
            <span className="text-[10px] text-[#904EE2] font-semibold bg-[#904EE214] border border-[#904EE233] px-1.5 py-0.2 rounded shrink-0">
              {issue.epicKey}
            </span>
          )}

          {issue.parentIssueKey && (
            <span className="text-[10px] text-[#626F86] font-mono bg-[#091E420F] px-1.5 py-0.2 rounded shrink-0">
              {issue.parentIssueKey}
            </span>
          )}
        </div>
      </div>

      {/* Right: Status badge + 3 Inline Editable Selectors */}
      <div className="flex flex-wrap items-center gap-2 shrink-0 self-end md:self-center">
        {/* Status Lozenge */}
        <div className="shrink-0">
          <StatusLozenge status={issue.status} size="sm" />
        </div>

        {/* 1. Inline Priority Dropdown */}
        <div className="relative shrink-0 flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <PriorityIcon priority={issue.priority} size={14} />
          <select
            disabled={!canEdit || isUpdatingPriority}
            value={issue.priority}
            onChange={(e) => onPriorityChange(issue, e.target.value as IssuePriority)}
            className={`text-xs font-medium px-2 py-1 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer hover:border-[#85B8FF] transition-colors ${
              isUpdatingPriority ? "opacity-50 animate-pulse" : ""
            }`}
            title="Sửa độ ưu tiên"
          >
            <option value="LOW">Thấp</option>
            <option value="MEDIUM">Trung bình</option>
            <option value="HIGH">Cao</option>
            <option value="CRITICAL">Khẩn cấp</option>
          </select>
        </div>

        {/* 2. Inline Assignee Dropdown */}
        <div className="relative shrink-0 flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
          <JiraAvatar name={issue.assignee?.displayName} size="xs" />
          <select
            disabled={!canEdit || isUpdatingAssignee}
            value={issue.assignee ? issue.assignee.id : ""}
            onChange={(e) => onAssigneeChange(issue, e.target.value ? Number(e.target.value) : null)}
            className={`text-xs font-medium px-2 py-1 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer max-w-[130px] truncate hover:border-[#85B8FF] transition-colors ${
              isUpdatingAssignee ? "opacity-50 animate-pulse" : ""
            }`}
            title="Giao người nhận việc"
          >
            <option value="">Chưa giao</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.fullname || m.username}
              </option>
            ))}
          </select>
        </div>

        {/* 3. Inline Sprint Dropdown */}
        {issue.type !== "EPIC" && issue.type !== "SUBTASK" ? (
          <div className="relative shrink-0" onClick={(e) => e.stopPropagation()}>
            <select
              disabled={!canManageSprint || isUpdatingSprint}
              value={issue.sprintId || ""}
              onChange={(e) => onSprintChange(issue, e.target.value ? Number(e.target.value) : null)}
              className={`text-xs font-medium px-2 py-1 rounded border border-[#091E4224] bg-white text-[#44546F] focus:outline-hidden focus:border-[#0C66E4] cursor-pointer max-w-[125px] truncate hover:border-[#85B8FF] transition-colors ${
                isUpdatingSprint ? "opacity-50 animate-pulse" : ""
              } ${!canManageSprint ? "cursor-not-allowed opacity-60 bg-[#FAFBFC]" : ""}`}
              title={
                !canManageSprint
                  ? "Chỉ Quản trị viên hoặc Quản lý mới có quyền đổi Sprint"
                  : "Đổi Sprint hoặc đưa về Backlog"
              }
            >
              <option value="">Backlog</option>
              {sprints
                .filter((s) => s.status !== "CLOSED")
                .map((sp) => (
                  <option key={sp.id} value={sp.id}>
                    {sp.name}
                  </option>
                ))}
            </select>
          </div>
        ) : (
          <div
            className="text-[10px] text-[#626F86] px-2 py-1 font-semibold uppercase bg-[#091E4208] rounded border border-transparent"
            title={issue.type === "EPIC" ? "Epic không thuộc Sprint" : "Subtask đi theo Story cha"}
          >
            {issue.type === "EPIC" ? "Epic" : "Subtask"}
          </div>
        )}
      </div>
    </div>
  );
}

// Jira Skeleton Loading Component
function IssueListSkeleton() {
  return (
    <div className="space-y-4">
      {[1, 2].map((s) => (
        <div key={s} className="bg-white rounded-lg border border-[#091E4224] overflow-hidden">
          <div className="p-3 bg-[#FAFBFC] border-b border-[#091E4214] flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-4 h-4 bg-[#091E420F] rounded animate-pulse" />
              <div className="w-28 h-5 bg-[#091E420F] rounded animate-pulse" />
              <div className="w-16 h-4 bg-[#091E420F] rounded-full animate-pulse" />
            </div>
            <div className="w-20 h-4 bg-[#091E420F] rounded animate-pulse" />
          </div>
          <div className="divide-y divide-[#091E4214] p-1">
            {[1, 2, 3].map((r) => (
              <div key={r} className="p-3 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3 flex-1">
                  <div className="w-4 h-4 bg-[#091E420F] rounded animate-pulse" />
                  <div className="w-14 h-4 bg-[#091E420F] rounded animate-pulse" />
                  <div className="w-64 h-4 bg-[#091E420F] rounded animate-pulse" />
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-16 h-5 bg-[#091E420F] rounded animate-pulse" />
                  <div className="w-20 h-6 bg-[#091E420F] rounded animate-pulse" />
                  <div className="w-24 h-6 bg-[#091E420F] rounded animate-pulse" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
