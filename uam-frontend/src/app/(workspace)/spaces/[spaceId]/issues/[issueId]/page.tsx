"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Issue, IssueStatus, IssuePriority, IssueHistory, IssueSummary, SpaceMember, Sprint } from "@/types/issue";
import { Team, TeamMember } from "@/types/team";
import { issueService } from "@/services/issue.service";
import { spaceService } from "@/services/space.service";
import { teamService } from "@/services/team.service";
import { sprintService } from "@/services/sprint.service";
import { extractErrorMessage } from "@/lib/api-client";
import { IssueTypeIcon, PriorityIcon, StatusLozenge, JiraAvatar } from "@/components/jira/JiraIcons";
import EditIssueModal from "@/components/issues/EditIssueModal";

interface PageProps {
  params: Promise<{ spaceId: string; issueId: string }>;
}

export default function IssueDetailPage({ params }: PageProps) {
  const resolvedParams = use(params);
  const spaceId = Number(resolvedParams.spaceId);
  const issueId = Number(resolvedParams.issueId);
  const router = useRouter();

  const [issue, setIssue] = useState<Issue | null>(null);
  const [subtasks, setSubtasks] = useState<IssueSummary[]>([]);
  const [epicChildren, setEpicChildren] = useState<IssueSummary[]>([]);
  const [auditHistory, setAuditHistory] = useState<IssueHistory[]>([]);

  // Metadata for edit modal & inline actions
  const [members, setMembers] = useState<SpaceMember[]>([]);
  const [filteredMembers, setFilteredMembers] = useState<SpaceMember[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedTeamFilter, setSelectedTeamFilter] = useState<number | "">("");
  const [epics, setEpics] = useState<IssueSummary[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Modals
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const fetchDetail = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await issueService.getIssueDetail(issueId);
      setIssue(data);

      if (data.type === "EPIC") {
        const children = await issueService.searchIssues({ spaceId, epicId: issueId, size: 50 });
        setEpicChildren(children.content);
      } else {
        const subs = await issueService.searchIssues({ spaceId, parentIssueId: issueId, size: 50 });
        setSubtasks(subs.content);
      }

      const audit = await issueService.getIssueAuditHistory(issueId, 0, 15);
      setAuditHistory(audit.content);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    (async () => {
      try {
        const data = await issueService.getIssueDetail(issueId);
        if (!isMounted) return;
        setIssue(data);

        if (data.type === "EPIC") {
          const children = await issueService.searchIssues({ spaceId, epicId: issueId, size: 50 });
          if (isMounted) setEpicChildren(children.content);
        } else {
          const subs = await issueService.searchIssues({ spaceId, parentIssueId: issueId, size: 50 });
          if (isMounted) setSubtasks(subs.content);
        }

        const audit = await issueService.getIssueAuditHistory(issueId, 0, 15);
        if (isMounted) setAuditHistory(audit.content);
      } catch (err) {
        if (isMounted) setError(extractErrorMessage(err));
      } finally {
        if (isMounted) setLoading(false);
      }
    })();

    spaceService.getMembers(spaceId, undefined, 0, 100).then((res) => {
      if (isMounted) {
        const eligible = res.content.filter((m: SpaceMember) => m.eligibleForAssign);
        setMembers(eligible);
        setFilteredMembers(eligible);
      }
    }).catch(() => {});

    teamService.getMyTeams().then((res) => {
      if (isMounted) setTeams(res || []);
    }).catch(() => {});

    issueService.searchIssues({ spaceId, type: "EPIC", size: 50 }).then((res) => {
      if (isMounted) setEpics(res.content);
    }).catch(() => {});

    sprintService.getSprints(spaceId, undefined, 0, 50).then((res) => {
      if (isMounted) setSprints(res.content.filter((s: Sprint) => s.status !== "CLOSED"));
    }).catch(() => {});

    return () => {
      isMounted = false;
    };
  }, [spaceId, issueId]);

  const handleTeamFilterChange = (teamId: number | "") => {
    setSelectedTeamFilter(teamId);
    if (!teamId) {
      setFilteredMembers(members);
      return;
    }

    teamService.getTeamMembers(Number(teamId), 0, 100).then((teamRes) => {
      const teamUserIds = new Set(teamRes.content.map((tm: TeamMember) => tm.userId));
      setFilteredMembers(members.filter((sm) => teamUserIds.has(sm.userId)));
    }).catch(() => {
      setFilteredMembers(members);
    });
  };

  const handleStatusChange = async (targetStatus: IssueStatus) => {
    if (!issue || targetStatus === issue.status) return;
    setActionLoading(true);
    setError(null);
    try {
      const updated = await issueService.changeStatus(issue.id, targetStatus, issue.version);
      setIssue(updated);
      fetchDetail();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handlePriorityChange = async (targetPriority: IssuePriority) => {
    if (!issue || targetPriority === issue.priority) return;
    setActionLoading(true);
    setError(null);
    try {
      const updated = await issueService.updateIssue(issue.id, {
        priority: targetPriority,
        version: issue.version,
      });
      setIssue(updated);
      fetchDetail();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleAssigneeChange = async (newAssigneeId: number | null) => {
    if (!issue) return;
    setActionLoading(true);
    setError(null);
    try {
      const updated = await issueService.updateIssue(issue.id, {
        assigneeId: newAssigneeId,
        version: issue.version,
      });
      setIssue(updated);
      fetchDetail();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!issue) return;
    setActionLoading(true);
    setError(null);
    try {
      await issueService.deleteIssue(issue.id, issue.version);
      router.push(`/spaces/${spaceId}/issues`);
    } catch (err) {
      setError(extractErrorMessage(err));
      setShowDeleteModal(false);
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto p-4 sm:p-6 space-y-6 animate-jira-fade">
        <div className="h-6 w-48 bg-[#EBECF0] rounded animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-4">
            <div className="h-40 bg-[#EBECF0] rounded-lg animate-pulse" />
            <div className="h-32 bg-[#EBECF0] rounded-lg animate-pulse" />
          </div>
          <div className="h-64 bg-[#EBECF0] rounded-lg animate-pulse" />
        </div>
      </div>
    );
  }

  if (!issue) {
    return (
      <div className="max-w-5xl mx-auto p-6">
        <div className="p-4 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-lg text-sm">
          {error || "Không tìm thấy công việc"}
        </div>
      </div>
    );
  }

  const canEdit = issue.allowedActions?.includes("EDIT") || issue.allowedActions?.includes("UPDATE");
  const canDelete = issue.allowedActions?.includes("DELETE");

  // Calculate subtasks completion percentage
  const completedSubtasks = subtasks.filter((s) => s.status === "DONE").length;
  const subtaskProgress = subtasks.length > 0 ? Math.round((completedSubtasks / subtasks.length) * 100) : 0;

  return (
    <div className="max-w-5xl mx-auto space-y-5 animate-jira-fade">
      {/* Breadcrumb Navigation & Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-[#091E4224] gap-3">
        <div className="flex items-center gap-2 text-xs font-semibold text-[#0C66E4] flex-wrap">
          <Link href={`/spaces/${spaceId}/issues`} className="hover:underline flex items-center gap-1 font-medium">
            <span>&larr; Danh sách công việc</span>
          </Link>
          <span className="text-[#626F86]">/</span>
          {issue.parentIssueKey && (
            <>
              <Link href={`/spaces/${spaceId}/issues/${issue.parentIssueId}`} className="hover:underline font-mono">
                {issue.parentIssueKey}
              </Link>
              <span className="text-[#626F86]">/</span>
            </>
          )}
          <span className="font-mono text-[#172B4D]">{issue.key}</span>
        </div>

        <div className="flex items-center gap-2">
          {canEdit && (
            <button
              onClick={() => setShowEditModal(true)}
              className="px-3 py-1.5 text-xs text-[#0C66E4] hover:bg-[#DEEBFF] border border-[#0C66E4] rounded-md font-semibold transition-colors cursor-pointer jira-interactive-btn"
            >
              Chỉnh sửa
            </button>
          )}

          {canDelete && (
            <button
              onClick={() => setShowDeleteModal(true)}
              className="px-3 py-1.5 text-xs text-[#DE350B] hover:bg-[#FFEBE6] border border-[#FF8F73] rounded-md font-semibold transition-colors cursor-pointer jira-interactive-btn"
            >
              Xóa công việc
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
          {error}
        </div>
      )}

      {/* Main Content Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Summary, Description, Subtasks, Activity */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-lg border border-[#091E4224] p-5 sm:p-6 shadow-2xs">
            {/* Issue Type & Key header */}
            <div className="flex items-center gap-2 flex-wrap mb-2">
              <IssueTypeIcon type={issue.type} size={18} />
              <span className="text-xs font-bold text-[#626F86] uppercase tracking-wide">
                {issue.type}
              </span>
              <span className="font-mono text-xs font-bold text-[#0C66E4]">{issue.key}</span>
            </div>

            <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] tracking-tight leading-snug">
              {issue.summary}
            </h1>

            {/* Description Section */}
            <div className="mt-5 pt-4 border-t border-[#091E4214]">
              <h2 className="text-xs font-bold text-[#626F86] uppercase tracking-wider mb-2">
                Mô tả chi tiết
              </h2>
              {issue.description ? (
                <div className="text-sm text-[#172B4D] whitespace-pre-wrap bg-[#FAFBFC] p-3.5 rounded-md border border-[#091E4214] leading-relaxed">
                  {issue.description}
                </div>
              ) : (
                <p className="text-xs text-[#626F86] italic">Chưa có mô tả chi tiết.</p>
              )}
            </div>

            {/* Subtasks Section */}
            {issue.type !== "SUBTASK" && issue.type !== "EPIC" && (
              <div className="mt-6 pt-5 border-t border-[#091E4214]">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <h2 className="text-xs font-bold text-[#626F86] uppercase tracking-wider">
                      Công việc con (Subtasks)
                    </h2>
                    <span className="text-xs font-semibold text-[#626F86]">
                      {completedSubtasks}/{subtasks.length}
                    </span>
                  </div>
                  {!issue.status.includes("DONE") && !issue.status.includes("CANCELLED") && (
                    <Link
                      href={`/spaces/${spaceId}/issues/new?type=SUBTASK&parentId=${issue.id}`}
                      className="text-xs font-semibold text-[#0C66E4] hover:underline"
                    >
                      + Thêm Subtask
                    </Link>
                  )}
                </div>

                {/* Progress bar */}
                {subtasks.length > 0 && (
                  <div className="w-full bg-[#EBECF0] h-1.5 rounded-full overflow-hidden mb-3">
                    <div
                      className="bg-[#1F845A] h-full transition-all duration-300 rounded-full"
                      style={{ width: `${subtaskProgress}%` }}
                    />
                  </div>
                )}

                {subtasks.length === 0 ? (
                  <p className="text-xs text-[#626F86] italic">Chưa có Subtask nào.</p>
                ) : (
                  <div className="border border-[#091E4214] rounded-md divide-y divide-[#091E4214] overflow-hidden">
                    {subtasks.map((sub) => (
                      <div
                        key={sub.id}
                        className="p-2.5 flex items-center justify-between hover:bg-[#FAFBFC] text-xs transition-colors"
                      >
                        <div className="flex items-center gap-2 min-w-0 flex-1">
                          <IssueTypeIcon type={sub.type} size={14} />
                          <span className="font-mono text-[#0C66E4] font-semibold">{sub.key}</span>
                          <Link
                            href={`/spaces/${spaceId}/issues/${sub.id}`}
                            className="font-medium text-[#172B4D] hover:underline truncate"
                          >
                            {sub.summary}
                          </Link>
                        </div>
                        <div className="shrink-0 ml-2">
                          <StatusLozenge status={sub.status} size="sm" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Epic Children Section */}
            {issue.type === "EPIC" && (
              <div className="mt-6 pt-5 border-t border-[#091E4214]">
                <h2 className="text-xs font-bold text-[#626F86] uppercase tracking-wider mb-3">
                  Công việc thuộc Epic ({epicChildren.length})
                </h2>
                {epicChildren.length === 0 ? (
                  <p className="text-xs text-[#626F86] italic">Chưa có công việc nào gắn với Epic này.</p>
                ) : (
                  <div className="border border-[#091E4214] rounded-md divide-y divide-[#091E4214] overflow-hidden">
                    {epicChildren.map((child) => (
                      <div
                        key={child.id}
                        className="p-2.5 flex items-center justify-between hover:bg-[#FAFBFC] text-xs transition-colors"
                      >
                        <div className="flex items-center gap-2 min-w-0 flex-1">
                          <IssueTypeIcon type={child.type} size={14} />
                          <span className="font-mono text-[#0C66E4] font-semibold">{child.key}</span>
                          <Link
                            href={`/spaces/${spaceId}/issues/${child.id}`}
                            className="font-medium text-[#172B4D] hover:underline truncate"
                          >
                            {child.summary}
                          </Link>
                        </div>
                        <div className="shrink-0 ml-2">
                          <StatusLozenge status={child.status} size="sm" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Activity / Audit History Timeline */}
          <div className="bg-white rounded-lg border border-[#091E4224] p-5 sm:p-6 shadow-2xs">
            <h2 className="text-xs font-bold text-[#626F86] uppercase tracking-wider mb-4">
              Lịch sử thay đổi (Audit Log)
            </h2>
            {auditHistory.length === 0 ? (
              <p className="text-xs text-[#626F86] italic">Chưa có lịch sử thay đổi.</p>
            ) : (
              <div className="space-y-3.5 relative before:absolute before:inset-0 before:left-3 before:w-0.5 before:bg-[#091E4214]">
                {auditHistory.map((item) => (
                  <div key={item.id} className="relative flex items-start gap-3 pl-1 text-xs">
                    <div className="relative z-10">
                      <JiraAvatar name={item.actorDisplayName} size="xs" />
                    </div>
                    <div className="flex-1 bg-[#FAFBFC] p-2.5 rounded-md border border-[#091E4214]">
                      <div className="flex items-center gap-2 text-[#626F86] flex-wrap">
                        <span className="font-semibold text-[#172B4D]">{item.actorDisplayName}</span>
                        <span>•</span>
                        <span className="text-[11px] font-mono">
                          {new Date(item.createdAt).toLocaleString("vi-VN")}
                        </span>
                        <span className="px-1.5 py-0.2 bg-[#091E420F] text-[#44546F] rounded text-[10px] font-mono uppercase font-semibold">
                          {item.action}
                        </span>
                      </div>
                      <p className="mt-1 text-[#44546F] leading-normal">{item.changes}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Status & Details Card */}
        <div className="space-y-5">
          {/* Status Box with Lozenge Dropdown */}
          <div className="bg-white rounded-lg border border-[#091E4224] p-4 sm:p-5 shadow-2xs">
            <label className="text-xs font-bold text-[#626F86] uppercase tracking-wider block mb-2">
              Trạng thái (Status)
            </label>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <StatusLozenge status={issue.status} size="md" />
                {actionLoading && <span className="text-xs text-[#626F86] animate-pulse">Đang cập nhật...</span>}
              </div>
              <select
                disabled={actionLoading}
                value={issue.status}
                onChange={(e) => handleStatusChange(e.target.value as IssueStatus)}
                className="w-full text-xs font-medium px-3 py-2 rounded-md border border-[#091E4224] bg-white cursor-pointer focus:outline-hidden focus:border-[#0C66E4] hover:border-[#85B8FF] transition-colors"
              >
                <option value="TODO">Cần làm (TODO)</option>
                <option value="IN_PROGRESS">Đang làm (IN_PROGRESS)</option>
                <option value="DONE">Hoàn thành (DONE)</option>
                <option value="CANCELLED">Đã hủy (CANCELLED)</option>
              </select>
            </div>
          </div>

          {/* Details Metadata Card */}
          <div className="bg-white rounded-lg border border-[#091E4224] p-4 sm:p-5 shadow-2xs space-y-4 text-xs">
            <h3 className="font-bold text-[#172B4D] uppercase tracking-wider text-[11px] pb-2 border-b border-[#091E4214]">
              Thông tin chi tiết
            </h3>

            {/* Assignee */}
            <div className="space-y-1.5">
              <label className="text-[#626F86] font-semibold block">
                Người nhận việc (Assignee)
              </label>

              {/* Team filter dropdown */}
              <div className="flex items-center gap-1.5 mb-1">
                <span className="text-[10px] text-[#626F86] shrink-0">Lọc nhóm:</span>
                <select
                  value={selectedTeamFilter}
                  onChange={(e) => handleTeamFilterChange(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-[11px] px-2 py-1 border border-[#091E4224] rounded bg-[#FAFBFC] focus:outline-hidden"
                >
                  <option value="">Tất cả ({members.length})</option>
                  {teams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Assignee select */}
              <div className="flex items-center gap-2">
                <JiraAvatar name={issue.assignee?.displayName} size="sm" />
                <select
                  disabled={actionLoading || !canEdit}
                  value={issue.assignee ? issue.assignee.id : ""}
                  onChange={(e) => handleAssigneeChange(e.target.value ? Number(e.target.value) : null)}
                  className="w-full text-xs font-medium px-2.5 py-1.5 rounded-md border border-[#091E4224] bg-white cursor-pointer focus:outline-hidden focus:border-[#0C66E4] hover:border-[#85B8FF] transition-colors"
                >
                  <option value="">Chưa giao việc (Unassigned)</option>
                  {filteredMembers.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.fullname || m.username} (@{m.username})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Reporter */}
            <div>
              <span className="text-[#626F86] font-semibold block mb-1">Người báo cáo (Reporter)</span>
              <div className="flex items-center gap-2">
                <JiraAvatar name={issue.reporter.displayName} size="sm" />
                <span className="text-[#172B4D] font-medium">{issue.reporter.displayName}</span>
              </div>
            </div>

            {/* Priority */}
            <div>
              <label className="text-[#626F86] font-semibold block mb-1">
                Độ ưu tiên (Priority)
              </label>
              <div className="flex items-center gap-2">
                <PriorityIcon priority={issue.priority} size={16} />
                <select
                  disabled={actionLoading || !canEdit}
                  value={issue.priority}
                  onChange={(e) => handlePriorityChange(e.target.value as IssuePriority)}
                  className="w-full text-xs font-medium px-2.5 py-1.5 rounded-md border border-[#091E4224] bg-white cursor-pointer focus:outline-hidden focus:border-[#0C66E4] hover:border-[#85B8FF] transition-colors"
                >
                  <option value="LOW">Thấp (Low)</option>
                  <option value="MEDIUM">Trung bình (Medium)</option>
                  <option value="HIGH">Cao (High)</option>
                  <option value="CRITICAL">Khẩn cấp (Critical)</option>
                </select>
              </div>
            </div>

            {/* Epic */}
            {issue.epicKey && (
              <div>
                <span className="text-[#626F86] font-semibold block mb-1">Epic</span>
                <span className="text-[#904EE2] font-semibold bg-[#904EE214] border border-[#904EE233] px-2 py-0.5 rounded inline-block text-[11px]">
                  {issue.epicKey} - {issue.epicSummary}
                </span>
              </div>
            )}

            {/* Parent */}
            {issue.parentIssueKey && (
              <div>
                <span className="text-[#626F86] font-semibold block mb-1">Công việc cha (Parent)</span>
                <span className="text-[#0C66E4] font-medium font-mono">
                  {issue.parentIssueKey} - {issue.parentIssueSummary}
                </span>
              </div>
            )}

            {/* Sprint */}
            <div>
              <span className="text-[#626F86] font-semibold block mb-1">Sprint</span>
              <span className="text-[#172B4D] font-medium">
                {issue.sprintName || <span className="text-[#626F86] italic">Backlog</span>}
              </span>
            </div>

            {/* Estimates & Due date */}
            <div className="grid grid-cols-2 gap-3 pt-2 border-t border-[#091E4214]">
              <div>
                <span className="text-[#626F86] font-semibold block mb-1">Ước lượng</span>
                <span className="text-[#172B4D] font-medium">
                  {issue.estimateHours !== undefined && issue.estimateHours !== null
                    ? `${issue.estimateHours} giờ`
                    : "—"}
                </span>
              </div>

              <div>
                <span className="text-[#626F86] font-semibold block mb-1">Hạn chót</span>
                <span className="text-[#172B4D] font-medium">{issue.dueDate || "—"}</span>
              </div>
            </div>

            {/* Labels */}
            {issue.labels && issue.labels.length > 0 && (
              <div className="pt-2 border-t border-[#091E4214]">
                <span className="text-[#626F86] font-semibold block mb-1.5">Nhãn (Labels)</span>
                <div className="flex flex-wrap gap-1">
                  {issue.labels.map((l) => (
                    <span
                      key={l}
                      className="px-2 py-0.5 bg-[#091E420F] text-[#44546F] rounded text-[11px] font-medium"
                    >
                      {l}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Edit Issue Modal */}
      {showEditModal && (
        <EditIssueModal
          issue={issue}
          members={members}
          epics={epics}
          sprints={sprints}
          isOpen={showEditModal}
          onClose={() => setShowEditModal(false)}
          onSuccess={(updated) => {
            setIssue(updated);
            fetchDetail();
          }}
        />
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 animate-jira-fade">
          <div className="bg-white rounded-lg p-6 max-w-sm w-full shadow-xl border border-[#091E4224] animate-jira-modal">
            <h4 className="text-base font-bold text-[#172B4D] mb-2">Xóa công việc {issue.key}?</h4>
            <p className="text-xs text-[#44546F] mb-4 leading-relaxed">
              Bạn có chắc chắn muốn xóa vĩnh viễn công việc này không? Mọi dữ liệu liên quan sẽ không thể phục hồi.
            </p>
            <div className="flex justify-end gap-2.5">
              <button
                type="button"
                disabled={actionLoading}
                onClick={() => setShowDeleteModal(false)}
                className="px-3.5 py-1.5 text-xs text-[#44546F] hover:bg-[#091E420F] rounded font-medium cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleDelete}
                className="px-4 py-1.5 text-xs bg-[#DE350B] text-white rounded font-semibold hover:bg-[#BF2600] disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
              >
                {actionLoading ? "Đang xóa..." : "Xác nhận xóa"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
