"use client";

import { useState } from "react";
import { Issue, IssuePriority, SpaceMember, IssueSummary, Sprint } from "@/types/issue";
import { issueService } from "@/services/issue.service";
import { extractErrorMessage } from "@/lib/api-client";
import { IssueTypeIcon, PriorityIcon, JiraAvatar } from "@/components/jira/JiraIcons";

interface EditIssueModalProps {
  issue: Issue;
  members: SpaceMember[];
  epics: IssueSummary[];
  sprints: Sprint[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedIssue: Issue) => void;
}

export default function EditIssueModal({
  issue,
  members,
  epics,
  sprints,
  isOpen,
  onClose,
  onSuccess,
}: EditIssueModalProps) {
  const [summary, setSummary] = useState(issue.summary || "");
  const [description, setDescription] = useState(issue.description || "");
  const [priority, setPriority] = useState<IssuePriority>(issue.priority || "MEDIUM");
  const [assigneeId, setAssigneeId] = useState<number | "">(issue.assignee?.id || "");
  const [epicId, setEpicId] = useState<number | "">(issue.epicId || "");
  const [sprintId, setSprintId] = useState<number | "">(issue.sprintId || "");
  const [estimateHours, setEstimateHours] = useState(
    issue.estimateHours !== null && issue.estimateHours !== undefined ? String(issue.estimateHours) : ""
  );
  const [dueDate, setDueDate] = useState(issue.dueDate || "");
  const [labels, setLabels] = useState(issue.labels ? issue.labels.join(", ") : "");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!summary.trim()) {
      setError("Tiêu đề không được để trống");
      return;
    }

    setLoading(true);
    setError(null);

    const labelsList = labels.split(",").map((l) => l.trim()).filter(Boolean);

    try {
      const updated = await issueService.updateIssue(issue.id, {
        summary: summary.trim(),
        description: description.trim() || undefined,
        priority,
        assigneeId: assigneeId ? Number(assigneeId) : null,
        epicId: (issue.type !== "EPIC" && issue.type !== "SUBTASK") ? (epicId ? Number(epicId) : null) : undefined,
        sprintId: (issue.type !== "EPIC" && issue.type !== "SUBTASK") ? (sprintId ? Number(sprintId) : null) : undefined,
        estimateHours: estimateHours ? Number(estimateHours) : null,
        dueDate: dueDate || null,
        labels: labelsList,
        version: issue.version,
      });

      setLoading(false);
      onSuccess(updated);
      onClose();
    } catch (err) {
      setError(extractErrorMessage(err));
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 overflow-y-auto animate-jira-fade">
      <div className="bg-white rounded-xl shadow-2xl max-w-xl w-full my-8 p-6 border border-[#091E4224] animate-jira-modal">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#091E4224]">
          <div className="flex items-center gap-2">
            <IssueTypeIcon type={issue.type} size={18} />
            <h2 className="text-base font-bold text-[#172B4D]">
              Chỉnh sửa: <span className="font-mono text-[#0C66E4]">{issue.key}</span>
            </h2>
          </div>
          <button
            onClick={onClose}
            className="text-[#626F86] hover:text-[#172B4D] text-sm p-1.5 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="mt-3 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
              Tiêu đề <span className="text-[#DE350B]">*</span>
            </label>
            <input
              type="text"
              required
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              className="w-full text-sm font-medium border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4] focus:ring-1 focus:ring-[#0C66E4]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Mô tả chi tiết</label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Người nhận việc</label>
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value ? Number(e.target.value) : "")}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
              >
                <option value="">-- Chưa chỉ định --</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullname || m.username} (@{m.username})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {issue.type !== "EPIC" && issue.type !== "SUBTASK" && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Thuộc Epic</label>
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
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Sprint</label>
                <select
                  value={sprintId}
                  onChange={(e) => setSprintId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 bg-white focus:outline-hidden focus:border-[#0C66E4]"
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

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Ước lượng (Giờ)</label>
              <input
                type="number"
                min="0"
                step="0.5"
                value={estimateHours}
                onChange={(e) => setEstimateHours(e.target.value)}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Hạn chót</label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">Nhãn (Labels)</label>
            <input
              type="text"
              value={labels}
              onChange={(e) => setLabels(e.target.value)}
              className="w-full text-xs border border-[#091E4224] rounded-md px-2.5 py-1.5 focus:outline-hidden focus:border-[#0C66E4]"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-[#091E4224]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium cursor-pointer transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 text-xs bg-[#0C66E4] hover:bg-[#0055CC] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs transition-colors jira-interactive-btn"
            >
              {loading ? "Đang lưu..." : "Lưu thay đổi"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
