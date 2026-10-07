"use client";

import { useEffect, useState, use, useCallback } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { IssueType, IssuePriority, SpaceMember, IssueSummary, Sprint, Space } from "@/types/issue";
import { Team, TeamMember } from "@/types/team";
import { issueService } from "@/services/issue.service";
import { spaceService } from "@/services/space.service";
import { teamService } from "@/services/team.service";
import { sprintService } from "@/services/sprint.service";
import { extractErrorMessage } from "@/lib/api-client";
import { IssueTypeIcon, PriorityIcon, JiraAvatar } from "@/components/jira/JiraIcons";

interface PageProps {
  params: Promise<{ spaceId: string }>;
}

export default function NewIssuePage({ params }: PageProps) {
  const resolvedParams = use(params);
  const routeSpaceId = Number(resolvedParams.spaceId);
  const router = useRouter();
  const searchParams = useSearchParams();

  const preselectedType = (searchParams.get("type") as IssueType) || "TASK";
  const preselectedParentId = searchParams.get("parentId") ? Number(searchParams.get("parentId")) : "";

  // Available spaces and teams
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [spaceMembers, setSpaceMembers] = useState<SpaceMember[]>([]);
  const [filteredMembers, setFilteredMembers] = useState<SpaceMember[]>([]);
  const [epics, setEpics] = useState<IssueSummary[]>([]);
  const [potentialParents, setPotentialParents] = useState<IssueSummary[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);

  // Selected space and form fields
  const [selectedSpaceId, setSelectedSpaceId] = useState<number>(routeSpaceId);
  const [selectedTeamId, setSelectedTeamId] = useState<number | "">("");
  const [type, setType] = useState<IssueType>(preselectedType);
  const [summary, setSummary] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<IssuePriority>("MEDIUM");
  const [assigneeId, setAssigneeId] = useState<number | "">("");
  const [epicId, setEpicId] = useState<number | "">("");
  const [parentIssueId, setParentIssueId] = useState<number | "">(preselectedParentId);
  const [sprintId, setSprintId] = useState<number | "">("");
  const [estimateHours, setEstimateHours] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [labels, setLabels] = useState("");

  const [loading, setLoading] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadSpaceData = useCallback(async (spaceId: number) => {
    setDataLoading(true);
    try {
      const [membersRes, epicsRes, allIssuesRes, sprintsRes] = await Promise.all([
        spaceService.getMembers(spaceId, undefined, 0, 100),
        issueService.searchIssues({ spaceId, type: "EPIC", size: 50 }),
        issueService.searchIssues({ spaceId, size: 50 }),
        sprintService.getSprints(spaceId, undefined, 0, 50),
      ]);
      const eligible = membersRes.content.filter((m: SpaceMember) => m.eligibleForAssign);
      setSpaceMembers(eligible);
      setFilteredMembers(eligible);
      setEpics(epicsRes.content);
      setPotentialParents(
        allIssuesRes.content.filter(
          (i: IssueSummary) => i.type !== "EPIC" && i.type !== "SUBTASK" && i.status !== "DONE" && i.status !== "CANCELLED"
        )
      );
      setSprints(sprintsRes.content.filter((s: Sprint) => s.status !== "CLOSED"));
    } catch (err) {
      console.error("Error loading space dependencies:", err);
    } finally {
      setDataLoading(false);
    }
  }, []);

  // 1. Initial Load of Spaces, Teams, and default space data
  useEffect(() => {
    let isMounted = true;

    spaceService.getMySpaces(0, 50).then((res) => {
      if (isMounted) setSpaces(res.content);
    }).catch(() => {});

    teamService.getMyTeams().then((res) => {
      if (isMounted) setTeams(res || []);
    }).catch(() => {});

    loadSpaceData(routeSpaceId);

    return () => {
      isMounted = false;
    };
  }, [routeSpaceId, loadSpaceData]);

  // Handle Space Switch
  const handleSpaceChange = (newSpaceId: number) => {
    setSelectedSpaceId(newSpaceId);
    setSelectedTeamId("");
    setAssigneeId("");
    setEpicId("");
    setParentIssueId("");
    setSprintId("");
    loadSpaceData(newSpaceId);
  };

  // Handle Team filter change for member selection
  const handleTeamChange = async (teamId: number | "") => {
    setSelectedTeamId(teamId);
    if (!teamId) {
      setFilteredMembers(spaceMembers);
      return;
    }

    try {
      const teamRes = await teamService.getTeamMembers(Number(teamId), 0, 100);
      const teamUserIds = new Set(teamRes.content.map((tm: TeamMember) => tm.userId));
      const filtered = spaceMembers.filter((sm) => teamUserIds.has(sm.userId));
      setFilteredMembers(filtered);
      if (assigneeId && !teamUserIds.has(Number(assigneeId))) {
        setAssigneeId("");
      }
    } catch {
      setFilteredMembers(spaceMembers);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!summary.trim()) {
      setError("Tiêu đề không được để trống");
      return;
    }

    if (type === "SUBTASK" && !parentIssueId) {
      setError("Subtask bắt buộc phải có công việc cha");
      return;
    }

    setLoading(true);
    setError(null);

    const labelsList = labels.split(",").map((l) => l.trim()).filter(Boolean);

    try {
      const payload = {
        spaceId: selectedSpaceId,
        type,
        summary: summary.trim(),
        description: description.trim() || undefined,
        priority,
        assigneeId: assigneeId ? Number(assigneeId) : undefined,
        epicId: (type !== "EPIC" && type !== "SUBTASK" && epicId) ? Number(epicId) : undefined,
        parentIssueId: (type === "SUBTASK" && parentIssueId) ? Number(parentIssueId) : undefined,
        sprintId: (type !== "EPIC" && type !== "SUBTASK" && sprintId) ? Number(sprintId) : undefined,
        estimateHours: estimateHours ? Number(estimateHours) : undefined,
        dueDate: dueDate || undefined,
        labels: labelsList,
      };

      const created = await issueService.createIssue(payload);
      router.push(`/spaces/${selectedSpaceId}/issues/${created.id}`);
    } catch (err) {
      setError(extractErrorMessage(err));
      setLoading(false);
    }
  };

  const currentSpace = spaces.find((s) => s.id === selectedSpaceId);

  return (
    <div className="max-w-3xl mx-auto space-y-5 animate-jira-fade">
      {/* Header & Back Link */}
      <div className="flex items-center gap-2 text-xs font-semibold text-[#0C66E4]">
        <Link href={`/spaces/${selectedSpaceId}/issues`} className="hover:underline flex items-center gap-1 font-medium">
          <span>&larr; Quay lại danh sách công việc</span>
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-[#091E4224] p-6 shadow-2xs">
        <h1 className="text-xl font-bold text-[#172B4D] mb-5 tracking-tight">Tạo công việc mới</h1>

        {error && (
          <div className="mb-4 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Space Selector & Issue Type Selector */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Không gian làm việc (Space) <span className="text-[#DE350B]">*</span>
              </label>
              <div className="flex items-center gap-2 border border-[#091E4224] rounded-md px-3 py-2 bg-white">
                <div className="w-5 h-5 rounded bg-[#0C66E4] text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                  {currentSpace?.key ? currentSpace.key.slice(0, 3) : "SP"}
                </div>
                <select
                  value={selectedSpaceId}
                  onChange={(e) => handleSpaceChange(Number(e.target.value))}
                  className="w-full text-xs font-semibold text-[#172B4D] bg-transparent border-none focus:outline-hidden cursor-pointer"
                >
                  {spaces.map((sp) => (
                    <option key={sp.id} value={sp.id}>
                      [{sp.key}] {sp.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Loại công việc <span className="text-[#DE350B]">*</span>
              </label>
              <div className="flex items-center gap-2 border border-[#091E4224] rounded-md px-3 py-2 bg-white">
                <IssueTypeIcon type={type} size={16} />
                <select
                  value={type}
                  onChange={(e) => {
                    const newType = e.target.value as IssueType;
                    setType(newType);
                    if (newType === "EPIC") {
                      setEpicId("");
                      setParentIssueId("");
                      setSprintId("");
                    } else if (newType === "SUBTASK") {
                      setEpicId("");
                    } else {
                      setParentIssueId("");
                    }
                  }}
                  className="w-full text-xs font-semibold text-[#172B4D] bg-transparent border-none focus:outline-hidden cursor-pointer"
                >
                  <option value="EPIC">Epic</option>
                  <option value="STORY">User Story</option>
                  <option value="TASK">Task</option>
                  <option value="BUG">Bug</option>
                  <option value="SUBTASK">Subtask</option>
                </select>
              </div>
            </div>
          </div>

          {/* Summary */}
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
              Tiêu đề <span className="text-[#DE350B]">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="VD: Cập nhật luồng xác thực hai bước..."
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              className="w-full text-sm font-medium border border-[#091E4224] rounded-md px-3.5 py-2.5 focus:outline-hidden focus:border-[#0C66E4] focus:ring-1 focus:ring-[#0C66E4]"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Mô tả</label>
            <textarea
              rows={4}
              placeholder="Thêm mô tả chi tiết cho công việc này..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
            />
          </div>

          {/* Subtask Specific: Mandatory Parent Linkage */}
          {type === "SUBTASK" && (
            <div className="p-3 bg-[#FAFBFC] border border-[#091E4224] rounded-md">
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Công việc cha (Parent Story / Task) <span className="text-[#DE350B]">*</span>
              </label>
              <select
                required
                value={parentIssueId}
                onChange={(e) => setParentIssueId(e.target.value ? Number(e.target.value) : "")}
                className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
              >
                <option value="">-- Chọn công việc cha --</option>
                {potentialParents.map((p) => (
                  <option key={p.id} value={p.id}>
                    [{p.key}] ({p.type}) {p.summary}
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-[#626F86] mt-1">
                Subtask bắt buộc phải thuộc về một User Story hoặc Task cụ thể.
              </p>
            </div>
          )}

          {/* Epic and Sprint Linkage */}
          {type !== "EPIC" && type !== "SUBTASK" && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-3 bg-[#FAFBFC] border border-[#091E4224] rounded-md">
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Epic</label>
                <select
                  value={epicId}
                  onChange={(e) => setEpicId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                >
                  <option value="">-- Không có Epic --</option>
                  {epics.map((ep) => (
                    <option key={ep.id} value={ep.id}>
                      [{ep.key}] {ep.summary}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Sprint</label>
                <select
                  value={sprintId}
                  onChange={(e) => setSprintId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                >
                  <option value="">-- Backlog --</option>
                  {sprints.map((sp) => (
                    <option key={sp.id} value={sp.id}>
                      {sp.name} ({sp.status})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* Team Filter & Assignee */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Lọc thành viên theo Nhóm (Team)
              </label>
              <select
                value={selectedTeamId}
                onChange={(e) => handleTeamChange(e.target.value ? Number(e.target.value) : "")}
                className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
              >
                <option value="">Tất cả thành viên ({spaceMembers.length})</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Người nhận việc (Assignee)
              </label>
              <div className="flex items-center gap-2">
                <select
                  value={assigneeId}
                  onChange={(e) => setAssigneeId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                >
                  <option value="">-- Chưa gán thành viên --</option>
                  {filteredMembers.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.fullname || m.username} (@{m.username})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Priority, Estimate Hours & Due Date */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Độ ưu tiên</label>
              <div className="flex items-center gap-1.5">
                <PriorityIcon priority={priority} size={15} />
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as IssuePriority)}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                >
                  <option value="LOW">Thấp (Low)</option>
                  <option value="MEDIUM">Trung bình (Medium)</option>
                  <option value="HIGH">Cao (High)</option>
                  <option value="CRITICAL">Khẩn cấp (Critical)</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Giờ ước lượng</label>
              <input
                type="number"
                min="0"
                step="0.5"
                placeholder="4, 8..."
                value={estimateHours}
                onChange={(e) => setEstimateHours(e.target.value)}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Hạn hoàn thành</label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
              />
            </div>
          </div>

          {/* Labels */}
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
              Nhãn (cách nhau bởi dấu phẩy)
            </label>
            <input
              type="text"
              placeholder="frontend, backend, auth..."
              value={labels}
              onChange={(e) => setLabels(e.target.value)}
              className="w-full text-xs border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
            />
          </div>

          {/* Submit */}
          <div className="flex justify-end gap-2.5 pt-4 border-t border-[#091E4224]">
            <Link
              href={`/spaces/${selectedSpaceId}/issues`}
              className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium"
            >
              Hủy
            </Link>
            <button
              type="submit"
              disabled={loading || dataLoading}
              className="px-5 py-2 text-xs bg-[#0C66E4] hover:bg-[#0055CC] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs transition-colors jira-interactive-btn"
            >
              {loading ? "Đang tạo..." : "Tạo công việc"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
