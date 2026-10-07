"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { teamService } from "@/services/team.service";
import { extractErrorMessage } from "@/lib/api-client";
import type { Team, TeamRole } from "@/types/team";

export default function TeamsPage() {
  const router = useRouter();
  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  // Modal create team state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchTeams = async () => {
    setIsLoading(true);
    setPageError(null);
    try {
      const data = await teamService.getMyTeams();
      setTeams(data);
    } catch (err) {
      setPageError(extractErrorMessage(err, "Không thể tải danh sách nhóm. Vui lòng thử lại."));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTeams();
  }, []);

  const handleOpenModal = () => {
    setName("");
    setDescription("");
    setNameError(null);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleCreateTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    setNameError(null);
    setModalError(null);

    const trimmedName = name.trim();
    if (!trimmedName) {
      setNameError("Tên nhóm không được để trống.");
      return;
    }
    if (trimmedName.length < 2) {
      setNameError("Tên nhóm phải có ít nhất 2 ký tự.");
      return;
    }
    if (trimmedName.length > 100) {
      setNameError("Tên nhóm không được vượt quá 100 ký tự.");
      return;
    }
    if (description && description.length > 500) {
      setModalError("Mô tả nhóm không được vượt quá 500 ký tự.");
      return;
    }

    setIsSubmitting(true);
    try {
      const newTeam = await teamService.createTeam({
        name: trimmedName,
        description: description.trim() || undefined,
      });

      setIsModalOpen(false);
      // Navigate to the newly created team
      router.push(`/teams/${newTeam.id}`);
    } catch (err) {
      setModalError(extractErrorMessage(err, "Tạo nhóm thất bại. Vui lòng thử lại."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const getRoleBadgeVariant = (role: TeamRole): "primary" | "info" | "neutral" => {
    switch (role) {
      case "OWNER":
        return "primary";
      case "ADMIN":
        return "info";
      case "MEMBER":
      default:
        return "neutral";
    }
  };

  const getRoleLabel = (role: TeamRole) => {
    switch (role) {
      case "OWNER":
        return "Chủ sở hữu";
      case "ADMIN":
        return "Quản trị viên";
      case "MEMBER":
      default:
        return "Thành viên";
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-[#DCDFE4] pb-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] leading-tight">
            Nhóm làm việc
          </h1>
          <p className="mt-1 text-xs sm:text-sm text-[#626F86]">
            Quản lý các nhóm bạn tham gia và tạo không gian làm việc cộng tác
          </p>
        </div>
        <Button onClick={handleOpenModal} className="shrink-0 self-start sm:self-auto">
          <svg className="w-4 h-4 mr-1.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Tạo nhóm mới
        </Button>
      </div>

      {/* Page Error Alert */}
      {pageError && (
        <Alert type="error" onClose={() => setPageError(null)}>
          {pageError}
        </Alert>
      )}

      {/* Loading Skeleton */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="rounded border border-[#DCDFE4] bg-white p-4 space-y-3">
              <Skeleton className="h-5 w-2/3" />
              <Skeleton className="h-4 w-full" />
              <div className="flex justify-between items-center pt-2">
                <Skeleton className="h-5 w-20" />
                <Skeleton className="h-4 w-16" />
              </div>
            </div>
          ))}
        </div>
      ) : teams.length === 0 ? (
        /* Empty State */
        <EmptyState
          title="Chưa tham gia nhóm nào"
          description="Bạn chưa là thành viên của bất kỳ nhóm làm việc nào. Hãy tạo nhóm mới để bắt đầu."
          action={
            <Button size="sm" onClick={handleOpenModal}>
              Tạo nhóm đầu tiên
            </Button>
          }
        />
      ) : (
        /* Team Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {teams.map((team) => (
            <Link
              key={team.id}
              href={`/teams/${team.id}`}
              className="group block rounded border border-[#DCDFE4] bg-white p-4 hover:border-[#0C66E4] hover:shadow-sm transition-all"
            >
              <div className="flex items-start justify-between gap-2 mb-2">
                <h3 className="font-semibold text-[#172B4D] group-hover:text-[#0C66E4] transition-colors truncate">
                  {team.name}
                </h3>
                <Badge variant={getRoleBadgeVariant(team.currentUserRole)}>
                  {getRoleLabel(team.currentUserRole)}
                </Badge>
              </div>

              <p className="text-xs text-[#626F86] line-clamp-2 min-h-[32px] mb-4">
                {team.description || "Chưa có mô tả cho nhóm này."}
              </p>

              <div className="flex items-center justify-between text-[11px] text-[#626F86] pt-2 border-t border-[#EBECF0]">
                <span className="flex items-center gap-1">
                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.75}
                      d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
                    />
                  </svg>
                  {team.memberCount} thành viên
                </span>
                <span>
                  {new Date(team.createdAt).toLocaleDateString("vi-VN", {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Modal Tạo nhóm mới */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !isSubmitting && setIsModalOpen(false)}
        title="Tạo nhóm làm việc mới"
      >
        <form onSubmit={handleCreateTeam} className="space-y-4">
          {modalError && (
            <Alert type="error" onClose={() => setModalError(null)}>
              {modalError}
            </Alert>
          )}

          <div>
            <Input
              label="Tên nhóm *"
              id="team-name"
              placeholder="vd: Đội ngũ Frontend, Dự án Alpha"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(null);
              }}
              error={nameError || undefined}
              disabled={isSubmitting}
              autoFocus
            />
          </div>

          <div>
            <label htmlFor="team-desc" className="block text-xs font-semibold text-[#172B4D] mb-1">
              Mô tả nhóm (tùy chọn)
            </label>
            <textarea
              id="team-desc"
              rows={3}
              placeholder="Mô tả mục tiêu hoặc phạm vi làm việc của nhóm..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isSubmitting}
              className="w-full rounded border border-[#DCDFE4] bg-white px-3 py-2 text-xs text-[#172B4D] placeholder-[#626F86] focus:border-[#0C66E4] focus:outline-none focus:ring-1 focus:ring-[#0C66E4] disabled:bg-[#F1F2F4] disabled:cursor-not-allowed"
            />
            <p className="mt-1 text-[11px] text-[#626F86] text-right">
              {description.length}/500 ký tự
            </p>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-[#EBECF0]">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Tạo nhóm
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
