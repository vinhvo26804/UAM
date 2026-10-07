"use client";

import { useEffect, useState, useCallback } from "react";
import { IssueType, IssuePriority, SpaceMember, IssueSummary, Sprint, Space } from "@/types/issue";
import { Team, TeamMember } from "@/types/team";
import { issueService } from "@/services/issue.service";
import { spaceService } from "@/services/space.service";
import { teamService } from "@/services/team.service";
import { sprintService } from "@/services/sprint.service";
import { extractErrorMessage } from "@/lib/api-client";
import { IssueTypeIcon, PriorityIcon, JiraAvatar } from "@/components/jira/JiraIcons";

interface CreateIssueModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultSpaceId?: number;
  defaultType?: IssueType;
  defaultParentId?: number;
  defaultSprintId?: number | null;
  onSuccess?: (createdIssue: unknown) => void;
}

export default function CreateIssueModal({
  isOpen,
  onClose,
  defaultSpaceId,
  defaultType = "TASK",
  defaultParentId,
  defaultSprintId,
  onSuccess,
}: CreateIssueModalProps) {
  // Available collections
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [spaceMembers, setSpaceMembers] = useState<SpaceMember[]>([]);
  const [filteredMembers, setFilteredMembers] = useState<SpaceMember[]>([]);
  const [epics, setEpics] = useState<IssueSummary[]>([]);
  const [potentialParents, setPotentialParents] = useState<IssueSummary[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);

  // Selected form states
  const [selectedSpaceId, setSelectedSpaceId] = useState<number | "">(defaultSpaceId || "");
  const [selectedTeamId, setSelectedTeamId] = useState<number | "">("");
  const [type, setType] = useState<IssueType>(defaultType);
  const [summary, setSummary] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<IssuePriority>("MEDIUM");
  const [assigneeId, setAssigneeId] = useState<number | "">("");
  const [epicId, setEpicId] = useState<number | "">("");
  const [parentIssueId, setParentIssueId] = useState<number | "">(defaultParentId || "");
  const [sprintId, setSprintId] = useState<number | "">(defaultSprintId !== undefined && defaultSprintId !== null ? defaultSprintId : "");
  const [estimateHours, setEstimateHours] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [labels, setLabels] = useState("");

  // Behaviors
  const [createAnother, setCreateAnother] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState<string | null>(null);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

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

  // 1. Load user spaces and teams when modal opens
  useEffect(() => {
    let isMounted = true;
    if (!isOpen) return;

    spaceService.getMySpaces(0, 50).then((res) => {
      if (!isMounted) return;
      setSpaces(res.content);
      const targetId = defaultSpaceId || (res.content.length > 0 ? res.content[0].id : "");
      if (targetId) {
        setSelectedSpaceId(targetId);
        loadSpaceData(Number(targetId));
      }
    }).catch(() => {});

    teamService.getMyTeams().then((res) => {
      if (isMounted) setTeams(res || []);
    }).catch(() => {});

    return () => {
      isMounted = false;
    };
  }, [isOpen, defaultSpaceId, loadSpaceData]);

  // Handle Space Switch
  const handleSpaceChange = (newSpaceId: number | "") => {
    setSelectedSpaceId(newSpaceId);
    setSelectedTeamId("");
    setAssigneeId("");
    setEpicId("");
    setParentIssueId("");
    setSprintId("");
    if (newSpaceId) {
      loadSpaceData(Number(newSpaceId));
    } else {
      setSpaceMembers([]);
      setFilteredMembers([]);
      setEpics([]);
      setPotentialParents([]);
      setSprints([]);
    }
  };

  // Handle Team Filter Switch for Assignees
  const handleTeamChange = async (teamId: number | "") => {
    setSelectedTeamId(teamId);
    setAssigneeId("");
    if (!teamId) {
      setFilteredMembers(spaceMembers);
      return;
    }

    try {
      const teamRes = await teamService.getTeamMembers(Number(teamId), 0, 100);
      const teamUserIds = new Set(teamRes.content.map((tm: TeamMember) => tm.userId));
      const filtered = spaceMembers.filter((sm) => teamUserIds.has(sm.userId));
      setFilteredMembers(filtered);
    } catch (err) {
      console.error("Error filtering team members:", err);
      setFilteredMembers(spaceMembers);
    }
  };

  const handleTypeChange = (newType: IssueType) => {
    setType(newType);
    if (newType === "EPIC") {
      setEpicId("");
      setParentIssueId("");
      setSprintId("");
      setEstimateHours("");
    } else if (newType === "SUBTASK") {
      setEpicId("");
    } else {
      setParentIssueId("");
    }
  };

  const handleClose = () => {
    if (summary.trim() || description.trim()) {
      setShowCloseConfirm(true);
    } else {
      onClose();
    }
  };

  const handleConfirmClose = () => {
    setShowCloseConfirm(false);
    onClose();
  };

  if (!isOpen) return null;

  const currentSpace = spaces.find((s) => s.id === Number(selectedSpaceId));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSpaceId) {
      setError("Vui lòng chọn Không gian làm việc (Space)");
      return;
    }
    if (!summary.trim()) {
      setError("Tiêu đề không được để trống");
      return;
    }

    if (type === "SUBTASK" && !parentIssueId) {
      setError("Subtask bắt buộc phải gắn vào một công việc cha (Story hoặc Task)");
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessToast(null);

    const labelsList = labels.split(",").map((l) => l.trim()).filter(Boolean);

    try {
      const payload = {
        spaceId: Number(selectedSpaceId),
        type,
        summary: summary.trim(),
        description: description.trim() || undefined,
        priority: type !== "EPIC" ? priority : undefined,
        assigneeId: assigneeId ? Number(assigneeId) : undefined,
        epicId: (type !== "EPIC" && type !== "SUBTASK" && epicId) ? Number(epicId) : undefined,
        parentIssueId: (type === "SUBTASK" && parentIssueId) ? Number(parentIssueId) : undefined,
        sprintId: (type !== "EPIC" && type !== "SUBTASK" && sprintId) ? Number(sprintId) : undefined,
        estimateHours: (type !== "EPIC" && estimateHours) ? Number(estimateHours) : undefined,
        dueDate: dueDate || undefined,
        labels: labelsList,
      };

      const created = await issueService.createIssue(payload);
      setLoading(false);
      if (onSuccess) {
        onSuccess(created);
      }

      if (createAnother) {
        setSummary("");
        setDescription("");
        setAssigneeId("");
        setEpicId("");
        setParentIssueId("");
        setEstimateHours("");
        setDueDate("");
        setLabels("");
        setSuccessToast(`Đã tạo thành công công việc ${created.key}!`);
        setTimeout(() => setSuccessToast(null), 4000);
      } else {
        onClose();
      }
    } catch (err) {
      setError(extractErrorMessage(err));
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 overflow-y-auto animate-jira-fade">
      <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full my-6 p-6 animate-jira-modal border border-[#091E4224]">
        {/* Top Bar: Space & Issue Type Selector */}
        <div className="flex items-center justify-between pb-3 border-b border-[#091E4224] gap-3">
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Space Selector with Badge */}
            <div className="flex items-center gap-1.5 bg-[#FAFBFC] border border-[#091E4224] rounded-md px-2.5 py-1">
              <div className="w-5 h-5 rounded bg-[#0C66E4] text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                {currentSpace?.key ? currentSpace.key.slice(0, 3) : "SP"}
              </div>
              <select
                aria-label="Chọn Không gian làm việc"
                value={selectedSpaceId}
                onChange={(e) => handleSpaceChange(e.target.value ? Number(e.target.value) : "")}
                className="text-xs font-semibold text-[#172B4D] bg-transparent border-none focus:outline-hidden cursor-pointer max-w-[140px] truncate"
              >
                <option value="">-- Chọn Space --</option>
                {spaces.map((sp) => (
                  <option key={sp.id} value={sp.id}>
                    {sp.key} ({sp.name})
                  </option>
                ))}
              </select>
            </div>

            <span className="text-[#626F86] text-xs">/</span>

            {/* Issue Type Selector with Jira SVG Icon */}
            <div className="flex items-center gap-2 bg-[#FAFBFC] border border-[#091E4224] rounded-md px-2.5 py-1">
              <IssueTypeIcon type={type} size={15} />
              <select
                aria-label="Chọn Loại công việc"
                value={type}
                onChange={(e) => handleTypeChange(e.target.value as IssueType)}
                className="text-xs font-semibold text-[#172B4D] bg-transparent border-none focus:outline-hidden cursor-pointer"
              >
                <option value="EPIC">Epic</option>
                <option value="STORY">User Story</option>
                <option value="TASK">Task</option>
                <option value="BUG">Bug</option>
                <option value="SUBTASK">Subtask</option>
              </select>
            </div>
          </div>

          {/* Close button */}
          <button
            type="button"
            onClick={handleClose}
            className="text-[#626F86] hover:text-[#172B4D] text-base p-1.5 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Feedback alerts */}
        {error && (
          <div className="mt-3 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs flex items-center justify-between animate-jira-slide-down">
            <span>{error}</span>
            <button type="button" onClick={() => setError(null)} className="text-[#DE350B] font-bold ml-2">✕</button>
          </div>
        )}

        {successToast && (
          <div className="mt-3 p-3 bg-[#E3FCEF] border border-[#57D9A3] text-[#006644] rounded-md text-xs flex items-center justify-between animate-jira-slide-down">
            <span>{successToast}</span>
            <button type="button" onClick={() => setSuccessToast(null)} className="text-[#006644] font-bold ml-2">✕</button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Main Title / Summary */}
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
              Tiêu đề công việc (Summary) <span className="text-[#DE350B]">*</span>
            </label>
            <input
              type="text"
              required
              autoFocus
              placeholder="Cần thực hiện những gì? (Ví dụ: Thiết kế giao diện luồng thanh toán...)"
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              className="w-full text-sm font-medium border border-[#091E4224] rounded-md px-3.5 py-2.5 focus:outline-hidden focus:border-[#0C66E4] focus:ring-1 focus:ring-[#0C66E4] bg-white transition-colors"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
              Mô tả chi tiết (Description)
            </label>
            <textarea
              rows={3}
              placeholder="Thêm mô tả chi tiết, bối cảnh, các bước thực hiện hoặc tiêu chí chấp nhận..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4] bg-white transition-colors"
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
                className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 bg-white focus:outline-hidden focus:border-[#0C66E4]"
              >
                <option value="">-- Chọn công việc cha trực tiếp --</option>
                {potentialParents.map((p) => (
                  <option key={p.id} value={p.id}>
                    [{p.key}] ({p.type}) {p.summary}
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-[#626F86] mt-1">
                Subtask sẽ được gắn trực tiếp vào công việc cha và tự động kế thừa Sprint từ công việc cha.
              </p>
            </div>
          )}

          {/* Story, Task, Bug: Parent Epic & Sprint */}
          {type !== "EPIC" && type !== "SUBTASK" && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3 bg-[#FAFBFC] border border-[#091E4224] rounded-md">
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Thuộc Epic (Parent Epic)
                </label>
                <select
                  value={epicId}
                  onChange={(e) => setEpicId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
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
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Gán vào Sprint
                </label>
                <select
                  value={sprintId}
                  onChange={(e) => setSprintId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
                >
                  <option value="">-- Backlog (Chưa vào Sprint) --</option>
                  {sprints.map((sp) => (
                    <option key={sp.id} value={sp.id}>
                      {sp.name} ({sp.status})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* Assignee & Team Filter */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Lọc thành viên theo Team
              </label>
              <select
                value={selectedTeamId}
                onChange={(e) => handleTeamChange(e.target.value ? Number(e.target.value) : "")}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
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
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value ? Number(e.target.value) : "")}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
              >
                <option value="">-- Chưa giao việc (Unassigned) --</option>
                {filteredMembers.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullname || m.username} (@{m.username})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Secondary Fields: Priority, Estimates, Due Date */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {type !== "EPIC" ? (
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Độ ưu tiên (Priority)
                </label>
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
            ) : (
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Cấp độ
                </label>
                <div className="text-xs px-2.5 py-1.5 bg-[#F1F2F4] text-[#44546F] rounded border border-[#091E4214] font-medium">
                  Sáng kiến / Epic cấp Không gian
                </div>
              </div>
            )}

            {type !== "EPIC" ? (
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Ước lượng (Giờ)
                </label>
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
            ) : (
              <div />
            )}

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                Hạn chót (Due Date)
              </label>
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
              Nhãn (Labels - cách nhau bởi dấu phẩy)
            </label>
            <input
              type="text"
              placeholder="frontend, backend, auth, ui..."
              value={labels}
              onChange={(e) => setLabels(e.target.value)}
              className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
            />
          </div>

          {/* Footer Actions */}
          <div className="pt-4 border-t border-[#091E4224] flex flex-col sm:flex-row items-center justify-between gap-3">
            {/* Create Another Checkbox */}
            <label className="flex items-center gap-2 text-xs text-[#44546F] cursor-pointer select-none">
              <input
                type="checkbox"
                checked={createAnother}
                onChange={(e) => setCreateAnother(e.target.checked)}
                className="w-4 h-4 rounded border-[#091E4224] text-[#0C66E4] focus:ring-[#0C66E4]"
              />
              <span>Tạo tiếp công việc khác (Create another)</span>
            </label>

            {/* Buttons */}
            <div className="flex items-center gap-2.5 self-end sm:self-auto">
              <button
                type="button"
                onClick={handleClose}
                className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium cursor-pointer transition-colors"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={loading || dataLoading}
                className="px-5 py-2 text-xs bg-[#0C66E4] hover:bg-[#0055CC] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs transition-colors jira-interactive-btn"
              >
                {loading ? "Đang tạo..." : "Tạo công việc"}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Unsaved Changes Confirmation Modal */}
      {showCloseConfirm && (
        <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/60 p-4 animate-jira-fade">
          <div className="bg-white rounded-lg p-5 max-w-sm w-full shadow-xl border border-[#091E4224] animate-jira-modal">
            <h4 className="text-sm font-bold text-[#172B4D] mb-2">Hủy tạo công việc?</h4>
            <p className="text-xs text-[#44546F] mb-4">
              Nội dung bạn đã nhập sẽ bị mất nếu bạn đóng cửa sổ này. Bạn có chắc chắn muốn thoát?
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowCloseConfirm(false)}
                className="px-3 py-1.5 text-xs text-[#44546F] hover:bg-[#091E420F] rounded font-medium cursor-pointer"
              >
                Tiếp tục chỉnh sửa
              </button>
              <button
                type="button"
                onClick={handleConfirmClose}
                className="px-3 py-1.5 text-xs bg-[#DE350B] text-white rounded font-medium hover:bg-[#BF2600] cursor-pointer"
              >
                Bỏ thay đổi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
