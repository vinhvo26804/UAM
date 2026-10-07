"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { Sprint, SprintCompletionPreview, Space, IssueSummary } from "@/types/issue";
import { sprintService } from "@/services/sprint.service";
import { spaceService } from "@/services/space.service";
import { issueService } from "@/services/issue.service";
import { extractErrorMessage } from "@/lib/api-client";
import {
  IssueTypeIcon,
  PriorityIcon,
  StatusLozenge,
  SprintIcon,
  JiraAvatar,
} from "@/components/jira/JiraIcons";
import CreateIssueModal from "@/components/issues/CreateIssueModal";

interface PageProps {
  params: Promise<{ spaceId: string }>;
}

export default function SpaceSprintsPage({ params }: PageProps) {
  const resolvedParams = use(params);
  const spaceId = Number(resolvedParams.spaceId);

  const [space, setSpace] = useState<Space | null>(null);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [sprintIssues, setSprintIssues] = useState<Record<number, IssueSummary[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Modal Create Sprint
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [sprintName, setSprintName] = useState("");
  const [sprintGoal, setSprintGoal] = useState("");
  const [plannedStart, setPlannedStart] = useState("");
  const [plannedEnd, setPlannedEnd] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);

  // Modal Complete Sprint
  const [completingSprint, setCompletingSprint] = useState<Sprint | null>(null);
  const [preview, setPreview] = useState<SprintCompletionPreview | null>(null);
  const [targetSprintId, setTargetSprintId] = useState<number | "">("");
  const [completeError, setCompleteError] = useState<string | null>(null);

  // Create Issue Modal
  const [targetSprintForCreate, setTargetSprintForCreate] = useState<number | null>(null);

  const fetchSprintIssues = async (sprintList: Sprint[]) => {
    const issuesMap: Record<number, IssueSummary[]> = {};
    await Promise.all(
      sprintList.map(async (sp) => {
        try {
          const res = await issueService.searchIssues({ spaceId, sprintId: sp.id, size: 50 });
          issuesMap[sp.id] = res.content;
        } catch {
          issuesMap[sp.id] = [];
        }
      })
    );
    setSprintIssues(issuesMap);
  };

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [spaceData, sprintData] = await Promise.all([
        spaceService.getSpaceDetail(spaceId),
        sprintService.getSprints(spaceId, undefined, 0, 50),
      ]);
      setSpace(spaceData);
      setSprints(sprintData.content);
      await fetchSprintIssues(sprintData.content);
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
        const [spaceData, sprintData] = await Promise.all([
          spaceService.getSpaceDetail(spaceId),
          sprintService.getSprints(spaceId, undefined, 0, 50),
        ]);
        if (isMounted) {
          setSpace(spaceData);
          setSprints(sprintData.content);
          const issuesMap: Record<number, IssueSummary[]> = {};
          await Promise.all(
            sprintData.content.map(async (sp) => {
              try {
                const res = await issueService.searchIssues({ spaceId, sprintId: sp.id, size: 50 });
                issuesMap[sp.id] = res.content;
              } catch {
                issuesMap[sp.id] = [];
              }
            })
          );
          if (isMounted) setSprintIssues(issuesMap);
        }
      } catch (err) {
        if (isMounted) setError(extractErrorMessage(err));
      } finally {
        if (isMounted) setLoading(false);
      }
    })();

    return () => {
      isMounted = false;
    };
  }, [spaceId]);

  const handleCreateSprint = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!sprintName.trim()) return;

    setActionLoading(true);
    setCreateError(null);
    try {
      await sprintService.createSprint({
        spaceId,
        name: sprintName.trim(),
        goal: sprintGoal.trim() || undefined,
        plannedStartAt: plannedStart || undefined,
        plannedEndAt: plannedEnd || undefined,
      });
      setShowCreateModal(false);
      setSprintName("");
      setSprintGoal("");
      setPlannedStart("");
      setPlannedEnd("");
      fetchData();
    } catch (err) {
      setCreateError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartSprint = async (sprint: Sprint) => {
    setActionLoading(true);
    setError(null);
    try {
      await sprintService.startSprint(sprint.id, sprint.version);
      fetchData();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const openCompleteModal = async (sprint: Sprint) => {
    setCompletingSprint(sprint);
    setCompleteError(null);
    setActionLoading(true);
    try {
      const p = await sprintService.previewComplete(sprint.id);
      setPreview(p);
      if (p.suggestedTargetSprintId) {
        setTargetSprintId(p.suggestedTargetSprintId);
      }
    } catch (err) {
      setError(extractErrorMessage(err));
      setCompletingSprint(null);
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmComplete = async () => {
    if (!completingSprint || !preview) return;

    setActionLoading(true);
    setCompleteError(null);
    try {
      await sprintService.completeSprint(completingSprint.id, {
        version: completingSprint.version,
        spaceRevision: preview.spaceRevision,
        targetSprintId: targetSprintId ? Number(targetSprintId) : undefined,
      });
      setCompletingSprint(null);
      setPreview(null);
      fetchData();
    } catch (err) {
      setCompleteError(extractErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const futureSprints = sprints.filter((s) => s.status === "FUTURE" && s.id !== completingSprint?.id);

  return (
    <div className="max-w-6xl mx-auto space-y-5 animate-jira-fade">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-[#091E4224] gap-4">
        <div>
          <div className="flex items-center gap-1.5 text-xs text-[#626F86] mb-1">
            <Link href={`/spaces/${spaceId}/issues`} className="hover:text-[#0C66E4] font-medium transition-colors">
              &larr; Danh sách công việc
            </Link>
            <span>/</span>
            <span className="font-semibold text-[#172B4D]">{space?.key || "..."}</span>
            <span>/</span>
            <span>Chu kỳ Sprint</span>
          </div>
          <div className="flex items-center gap-2.5">
            <SprintIcon className="w-6 h-6 text-[#0C66E4]" />
            <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] tracking-tight">
              Quản lý Chu kỳ Sprint
            </h1>
          </div>
        </div>

        <button
          onClick={() => {
            setShowCreateModal(true);
            setCreateError(null);
          }}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded shadow-xs transition-colors self-start sm:self-auto cursor-pointer jira-interactive-btn"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
          </svg>
          Tạo Sprint mới
        </button>
      </div>

      {error && (
        <div className="p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
          {error}
        </div>
      )}

      {/* Sprints List */}
      <div className="space-y-5">
        {loading ? (
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="bg-white border border-[#091E4224] rounded-lg p-5 animate-pulse space-y-3">
                <div className="h-5 bg-[#091E420F] rounded w-1/3" />
                <div className="h-4 bg-[#091E420F] rounded w-1/2" />
                <div className="h-16 bg-[#091E420F] rounded" />
              </div>
            ))}
          </div>
        ) : sprints.length === 0 ? (
          <div className="py-16 text-center bg-white rounded-lg border border-[#091E4224] p-8 shadow-2xs">
            <div className="w-12 h-12 rounded-full bg-[#E9F2FF] text-[#0C66E4] flex items-center justify-center mx-auto mb-3">
              <SprintIcon className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#172B4D]">Không gian chưa có Sprint nào</h3>
            <p className="text-xs text-[#626F86] mt-1 max-w-sm mx-auto">
              Bắt đầu lập kế hoạch bằng cách tạo Sprint mới để quản lý chu kỳ phân phối công việc.
            </p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="mt-4 px-4 py-2 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded cursor-pointer transition-colors shadow-xs jira-interactive-btn inline-flex items-center gap-1.5"
            >
              + Tạo Sprint đầu tiên
            </button>
          </div>
        ) : (
          sprints.map((sprint) => {
            const issues = sprintIssues[sprint.id] || [];
            return (
              <div
                key={sprint.id}
                className="bg-white border border-[#091E4224] rounded-lg p-4 sm:p-5 shadow-2xs transition-shadow hover:shadow-xs"
              >
                {/* Sprint Header */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-[#091E4214]">
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`px-2 py-0.5 text-[10px] font-bold rounded uppercase tracking-wider ${
                          sprint.status === "ACTIVE"
                            ? "bg-[#E3FCEF] text-[#006644] border border-[#ABF5D1]"
                            : sprint.status === "CLOSED"
                            ? "bg-[#DFE1E6] text-[#42526E] border border-[#C1C7D0]"
                            : "bg-[#DEEBFF] text-[#0747A6] border border-[#B3D4FF]"
                        }`}
                      >
                        {sprint.status}
                      </span>
                      <h3 className="text-base font-bold text-[#172B4D]">{sprint.name}</h3>
                      <span className="text-xs font-semibold px-2 py-0.5 bg-[#091E420F] text-[#44546F] rounded">
                        {issues.length} công việc
                      </span>
                    </div>

                    {sprint.goal && (
                      <p className="text-xs text-[#44546F] mt-1.5">
                        <span className="font-semibold text-[#626F86]">Mục tiêu:</span> {sprint.goal}
                      </p>
                    )}

                    <div className="flex items-center gap-4 text-xs text-[#626F86] mt-2 font-mono">
                      <span>Vị trí: #{sprint.position}</span>
                      {sprint.startedAt && (
                        <span>Bắt đầu: {new Date(sprint.startedAt).toLocaleDateString("vi-VN")}</span>
                      )}
                      {sprint.completedAt && (
                        <span>Kết thúc: {new Date(sprint.completedAt).toLocaleDateString("vi-VN")}</span>
                      )}
                    </div>
                  </div>

                  {/* Sprint Actions */}
                  <div className="flex items-center gap-2 flex-wrap self-end md:self-center">
                    {sprint.status === "FUTURE" && (
                      <button
                        disabled={actionLoading}
                        onClick={() => handleStartSprint(sprint)}
                        className="px-3 py-1.5 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded transition-colors disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
                      >
                        Bắt đầu Sprint
                      </button>
                    )}

                    {sprint.status === "ACTIVE" && (
                      <button
                        disabled={actionLoading}
                        onClick={() => openCompleteModal(sprint)}
                        className="px-3 py-1.5 bg-[#1F845A] hover:bg-[#166644] text-white text-xs font-semibold rounded transition-colors disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
                      >
                        Hoàn thành Sprint
                      </button>
                    )}

                    <button
                      onClick={() => setTargetSprintForCreate(sprint.id)}
                      className="px-3 py-1.5 bg-[#091E420F] hover:bg-[#091E4214] text-[#172B4D] text-xs font-semibold rounded transition-colors cursor-pointer jira-interactive-btn"
                    >
                      + Thêm việc vào Sprint
                    </button>

                    <Link
                      href={`/spaces/${spaceId}/issues?sprintId=${sprint.id}`}
                      className="px-3 py-1.5 border border-[#091E4224] hover:bg-[#091E420F] text-[#44546F] text-xs font-semibold rounded transition-colors jira-interactive-btn"
                    >
                      Bộ lọc ({issues.length})
                    </Link>
                  </div>
                </div>

                {/* Issues in this Sprint */}
                <div className="mt-3">
                  {issues.length === 0 ? (
                    <div className="p-4 bg-[#FAFBFC] border border-dashed border-[#091E4224] rounded-md text-center">
                      <p className="text-xs font-semibold text-[#172B4D]">
                        Danh sách công việc của Sprint này đang rỗng (0 công việc).
                      </p>
                      <p className="text-[11px] text-[#626F86] mt-0.5">
                        Khi sprint được tạo, ds công việc là rỗng. Khi nào được gán thì công việc mới được hiển thị tại đây.
                      </p>
                      <button
                        onClick={() => setTargetSprintForCreate(sprint.id)}
                        className="mt-2 text-xs font-semibold text-[#0C66E4] hover:underline cursor-pointer"
                      >
                        + Gán hoặc tạo công việc mới vào {sprint.name}
                      </button>
                    </div>
                  ) : (
                    <div className="border border-[#091E4214] rounded-md divide-y divide-[#091E4214] overflow-hidden">
                      {issues.map((iss) => (
                        <div
                          key={iss.id}
                          className="p-2.5 flex items-center justify-between hover:bg-[#FAFBFC] text-xs transition-colors"
                        >
                          <div className="flex items-center gap-2.5 min-w-0 flex-1">
                            <IssueTypeIcon type={iss.type} size={15} />
                            <Link
                              href={`/spaces/${spaceId}/issues/${iss.id}`}
                              className="font-mono font-semibold text-[#0C66E4] hover:underline shrink-0"
                            >
                              {iss.key}
                            </Link>
                            <Link
                              href={`/spaces/${spaceId}/issues/${iss.id}`}
                              className="font-medium text-[#172B4D] hover:underline truncate"
                            >
                              {iss.summary}
                            </Link>
                          </div>

                          <div className="flex items-center gap-3 text-xs shrink-0 ml-2">
                            <div className="flex items-center gap-1">
                              <PriorityIcon priority={iss.priority} size={14} />
                              <span className="text-[#626F86] text-[11px] hidden sm:inline">{iss.priority}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                              <JiraAvatar name={iss.assignee?.displayName} size="xs" />
                              <span className="text-[#44546F] text-[11px] hidden md:inline truncate max-w-[100px]">
                                {iss.assignee ? iss.assignee.displayName : "Chưa giao"}
                              </span>
                            </div>
                            <StatusLozenge status={iss.status} size="sm" />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Modal Tạo Sprint */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-jira-fade">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 border border-[#091E4224] animate-jira-modal">
            <div className="flex items-center justify-between pb-3 border-b border-[#091E4224]">
              <div className="flex items-center gap-2">
                <SprintIcon className="w-5 h-5 text-[#0C66E4]" />
                <h2 className="text-base font-bold text-[#172B4D]">Tạo Sprint mới</h2>
              </div>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-[#626F86] hover:text-[#172B4D] text-sm p-1 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
              >
                ✕
              </button>
            </div>

            {createError && (
              <div className="mt-3 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
                {createError}
              </div>
            )}

            <form onSubmit={handleCreateSprint} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Tên Sprint <span className="text-[#DE350B]">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Sprint 1 - Phát triển tính năng cốt lõi"
                  value={sprintName}
                  onChange={(e) => setSprintName(e.target.value)}
                  className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Mục tiêu Sprint
                </label>
                <textarea
                  rows={2}
                  placeholder="Mục tiêu kinh doanh hoặc kỹ thuật cần đạt được trong chu kỳ..."
                  value={sprintGoal}
                  onChange={(e) => setSprintGoal(e.target.value)}
                  className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                    Ngày bắt đầu dự kiến
                  </label>
                  <input
                    type="date"
                    value={plannedStart}
                    onChange={(e) => setPlannedStart(e.target.value)}
                    className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                    Ngày kết thúc dự kiến
                  </label>
                  <input
                    type="date"
                    value={plannedEnd}
                    onChange={(e) => setPlannedEnd(e.target.value)}
                    className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-[#091E4224]">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2 text-xs bg-[#0C66E4] hover:bg-[#0055CC] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
                >
                  {actionLoading ? "Đang tạo..." : "Tạo Sprint"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Hoàn thành Sprint */}
      {completingSprint && preview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-jira-fade">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full p-6 border border-[#091E4224] animate-jira-modal">
            <h2 className="text-base font-bold text-[#172B4D] pb-3 border-b border-[#091E4224]">
              Hoàn thành Sprint: {completingSprint.name}
            </h2>

            {completeError && (
              <div className="mt-3 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
                {completeError}
              </div>
            )}

            <div className="mt-4 space-y-3 text-xs text-[#44546F]">
              <div className="p-3 bg-[#FAFBFC] rounded-md border border-[#091E4214] space-y-1.5">
                <p className="flex justify-between">
                  <span>Công việc hoàn thành giữ lại:</span>
                  <span className="font-bold text-[#1F845A]">{preview.stayCount}</span>
                </p>
                <p className="flex justify-between">
                  <span>Công việc chưa hoàn thành chuyển đi:</span>
                  <span className="font-bold text-[#DE350B]">{preview.moveIssueCount}</span>
                </p>
              </div>

              {preview.moveIssueCount > 0 && (
                <div className="pt-2">
                  <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                    Chuyển {preview.moveIssueCount} công việc chưa hoàn thành sang:
                  </label>
                  <select
                    value={targetSprintId}
                    onChange={(e) => setTargetSprintId(e.target.value ? Number(e.target.value) : "")}
                    className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                  >
                    <option value="">Backlog (Chưa vào Sprint)</option>
                    {futureSprints.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} (Tương lai)
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2.5 pt-4 mt-4 border-t border-[#091E4224]">
              <button
                type="button"
                onClick={() => {
                  setCompletingSprint(null);
                  setPreview(null);
                }}
                className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleConfirmComplete}
                className="px-5 py-2 text-xs bg-[#1F845A] hover:bg-[#166644] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
              >
                {actionLoading ? "Đang xử lý..." : "Xác nhận hoàn thành"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Issue Modal inside Sprint */}
      {targetSprintForCreate !== null && (
        <CreateIssueModal
          isOpen={targetSprintForCreate !== null}
          onClose={() => setTargetSprintForCreate(null)}
          defaultSpaceId={spaceId}
          defaultSprintId={targetSprintForCreate}
          onSuccess={() => {
            fetchData();
          }}
        />
      )}
    </div>
  );
}
