"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Badge } from "@/components/ui/badge";
import { Alert } from "@/components/ui/alert";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Pagination } from "@/components/ui/pagination";
import { teamService } from "@/services/team.service";
import { tokenStorage } from "@/lib/token-storage";
import { extractErrorMessage } from "@/lib/api-client";
import type { Team, TeamMember, TeamRole, CandidateUser } from "@/types/team";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function TeamDetailPage({ params }: PageProps) {
  const router = useRouter();
  const resolvedParams = use(params);
  const teamId = Number(resolvedParams.id);

  const [team, setTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [totalMembers, setTotalMembers] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Current logged in user info
  const currentLoggedUser = tokenStorage.getCurrentUser();

  // Modal add member state
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [candidates, setCandidates] = useState<CandidateUser[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [addingUserId, setAddingUserId] = useState<number | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  // Updating role state
  const [updatingUserId, setUpdatingUserId] = useState<number | null>(null);

  // Modal confirm remove / leave team
  const [memberToRemove, setMemberToRemove] = useState<TeamMember | null>(null);
  const [isRemoving, setIsRemoving] = useState(false);

  const canManageMembers =
    team?.currentUserRole === "OWNER" || team?.currentUserRole === "ADMIN";

  const fetchTeamData = async () => {
    if (isNaN(teamId) || teamId <= 0) {
      setPageError("Mã nhóm làm việc không hợp lệ.");
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setPageError(null);

    try {
      const [teamDetail, membersPage] = await Promise.all([
        teamService.getTeamDetail(teamId),
        teamService.getTeamMembers(teamId, currentPage - 1, pageSize),
      ]);

      setTeam(teamDetail);
      setMembers(membersPage.content);
      setTotalMembers(membersPage.totalElements);
    } catch (err) {
      setPageError(
        extractErrorMessage(
          err,
          "Không thể tải thông tin nhóm. Bạn có thể không có quyền truy cập hoặc nhóm không tồn tại."
        )
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamData();
  }, [teamId, currentPage, pageSize]);

  const handleOpenAddModal = async () => {
    setSearchQuery("");
    setCandidates([]);
    setModalError(null);
    setIsAddModalOpen(true);
    setIsSearching(true);
    try {
      const results = await teamService.searchCandidates(teamId, "");
      setCandidates(results);
    } catch (err) {
      setModalError(extractErrorMessage(err, "Không thể tải danh sách ứng viên"));
    } finally {
      setIsSearching(false);
    }
  };

  const handleSearchCandidates = async (query: string) => {
    setSearchQuery(query);
    setIsSearching(true);
    setModalError(null);
    try {
      const results = await teamService.searchCandidates(teamId, query);
      setCandidates(results);
    } catch (err) {
      setModalError(extractErrorMessage(err, "Tìm kiếm ứng viên thất bại"));
    } finally {
      setIsSearching(false);
    }
  };

  const handleAddMember = async (userId: number) => {
    setAddingUserId(userId);
    setModalError(null);
    try {
      await teamService.addMember(teamId, { userId });
      setCandidates((prev) => prev.filter((c) => c.id !== userId));
      setSuccessMessage("Đã thêm thành viên vào nhóm thành công.");
      fetchTeamData();
    } catch (err) {
      setModalError(extractErrorMessage(err, "Không thể thêm thành viên"));
    } finally {
      setAddingUserId(null);
    }
  };

  const handleRoleChange = async (userId: number, newRole: TeamRole) => {
    if (newRole === "OWNER") return;
    setUpdatingUserId(userId);
    setPageError(null);
    setSuccessMessage(null);

    try {
      const updated = await teamService.updateMemberRole(teamId, userId, { role: newRole });
      setMembers((prev) =>
        prev.map((m) => (m.userId === userId ? { ...m, role: updated.role } : m))
      );
      setSuccessMessage("Đã cập nhật vai trò thành viên thành công.");
    } catch (err) {
      setPageError(extractErrorMessage(err, "Không thể cập nhật vai trò thành viên"));
    } finally {
      setUpdatingUserId(null);
    }
  };

  const handleConfirmRemove = async () => {
    if (!memberToRemove) return;
    setIsRemoving(true);
    setPageError(null);
    setSuccessMessage(null);

    const isSelf = memberToRemove.username === currentLoggedUser?.username;

    try {
      await teamService.removeMember(teamId, memberToRemove.userId);
      setMemberToRemove(null);

      if (isSelf) {
        // If current user left the team, redirect to teams list
        router.push("/teams");
      } else {
        setSuccessMessage(`Đã xóa thành viên @${memberToRemove.username} ra khỏi nhóm thành công.`);
        fetchTeamData();
      }
    } catch (err) {
      setPageError(extractErrorMessage(err, "Không thể xóa thành viên khỏi nhóm"));
      setMemberToRemove(null);
    } finally {
      setIsRemoving(false);
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

  if (isLoading && !team) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-6 w-32" />
        <div className="rounded border border-[#DCDFE4] bg-white p-6 space-y-4">
          <Skeleton className="h-8 w-1/3" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-4 w-1/4" />
        </div>
        <div className="rounded border border-[#DCDFE4] bg-white p-6 space-y-4">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-32 w-full" />
        </div>
      </div>
    );
  }

  if (pageError && !team) {
    return (
      <div className="space-y-4">
        <Link
          href="/teams"
          className="inline-flex items-center text-xs font-semibold text-[#0C66E4] hover:underline"
        >
          ← Quay lại danh sách nhóm
        </Link>
        <Alert type="error">{pageError}</Alert>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back link */}
      <div>
        <Link
          href="/teams"
          className="inline-flex items-center text-xs font-semibold text-[#626F86] hover:text-[#0C66E4] transition-colors"
        >
          <svg
            className="w-3.5 h-3.5 mr-1"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M10 19l-7-7m0 0l7-7m-7 7h18"
            />
          </svg>
          Quay lại danh sách nhóm
        </Link>
      </div>

      {pageError && (
        <Alert type="error" onClose={() => setPageError(null)}>
          {pageError}
        </Alert>
      )}

      {successMessage && (
        <Alert type="success" onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {/* Team Header Card */}
      {team && (
        <div className="rounded border border-[#DCDFE4] bg-white p-6 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D]">
                  {team.name}
                </h1>
                <Badge variant={getRoleBadgeVariant(team.currentUserRole)}>
                  {getRoleLabel(team.currentUserRole)}
                </Badge>
              </div>

              <p className="mt-2 text-sm text-[#44546F] max-w-2xl whitespace-pre-wrap">
                {team.description || "Chưa có mô tả cho nhóm này."}
              </p>

              <div className="mt-4 flex items-center gap-4 text-xs text-[#626F86]">
                <span className="flex items-center gap-1.5">
                  <svg
                    className="w-4 h-4 text-[#626F86]"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.75}
                      d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
                    />
                  </svg>
                  {totalMembers} thành viên
                </span>
                <span>•</span>
                <span>
                  Ngày tạo:{" "}
                  {new Date(team.createdAt).toLocaleDateString("vi-VN", {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })}
                </span>
              </div>
            </div>

            {canManageMembers && (
              <Button onClick={handleOpenAddModal} className="shrink-0 self-start sm:self-auto">
                <svg className="w-4 h-4 mr-1.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>
                Thêm thành viên
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Members Section */}
      <div className="rounded border border-[#DCDFE4] bg-white overflow-hidden shadow-sm">
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#DCDFE4]">
          <div>
            <h2 className="text-base font-semibold text-[#172B4D]">
              Thành viên nhóm ({totalMembers})
            </h2>
            <p className="text-xs text-[#626F86]">
              Danh sách tất cả người dùng tham gia vào nhóm làm việc này
            </p>
          </div>
        </div>

        {/* Members Table */}
        {isLoading ? (
          <div className="p-6 space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}\
          </div>
        ) : members.length === 0 ? (
          <EmptyState
            title="Chưa có thành viên"
            description="Nhóm này hiện chưa có thành viên nào."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#DCDFE4] bg-[#FAFBFC] text-[#626F86] font-semibold">
                  <th className="py-3 px-4">Thành viên</th>
                  <th className="py-3 px-4">Email</th>
                  <th className="py-3 px-4">Vai trò</th>
                  <th className="py-3 px-4">Ngày tham gia</th>
                  {canManageMembers && <th className="py-3 px-4 text-center">Đổi vai trò</th>}
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DCDFE4]">
                {members.map((member) => {
                  const isOwner = member.role === "OWNER";
                  const isSelf = member.username === currentLoggedUser?.username;

                  // Permission check for changing role
                  const cannotEditRole =
                    isOwner ||
                    (team?.currentUserRole === "ADMIN" && member.role === "ADMIN");

                  // Permission check for removing member
                  // - Owner can remove anyone except self (OWNER cannot leave directly)
                  // - Admin can remove Members and self (cannot remove Owner or other Admins)
                  // - Member can only remove self (leave group)
                  let canRemove = false;
                  if (team?.currentUserRole === "OWNER") {
                    canRemove = !isOwner; // Owner can remove any Admin or Member
                  } else if (team?.currentUserRole === "ADMIN") {
                    canRemove = isSelf || member.role === "MEMBER";
                  } else if (team?.currentUserRole === "MEMBER") {
                    canRemove = isSelf;
                  }

                  return (
                    <tr key={member.id} className="hover:bg-[#FAFBFC] transition-colors">
                      <td className="py-3 px-4">
                        <div>
                          <div className="font-semibold text-[#172B4D] flex items-center gap-1.5">
                            <span>{member.fullname || member.username}</span>
                            {isSelf && (
                              <span className="text-[10px] text-[#0C66E4] bg-[#E9F2FF] px-1.5 py-0.5 rounded font-normal">
                                Bạn
                              </span>
                            )}
                          </div>
                          <div className="text-[11px] text-[#626F86]">
                            @{member.username}
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-[#44546F]">{member.email}</td>
                      <td className="py-3 px-4">
                        <Badge variant={getRoleBadgeVariant(member.role)}>
                          {getRoleLabel(member.role)}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-[#626F86]">
                        {new Date(member.joinedAt).toLocaleDateString("vi-VN", {
                          day: "2-digit",
                          month: "2-digit",
                          year: "numeric",
                        })}
                      </td>
                      {canManageMembers && (
                        <td className="py-3 px-4 text-center">
                          {isOwner ? (
                            <span className="text-[11px] text-[#626F86] italic">
                              Cố định
                            </span>
                          ) : (
                            <select
                              value={member.role}
                              disabled={cannotEditRole || updatingUserId === member.userId}
                              onChange={(e) =>
                                handleRoleChange(member.userId, e.target.value as TeamRole)
                              }
                              className="h-7 text-xs rounded border border-[#DCDFE4] bg-white px-2 text-[#172B4D] focus:outline-none focus:border-[#0C66E4] disabled:opacity-50 cursor-pointer"
                            >
                              <option value="MEMBER">Thành viên</option>
                              <option value="ADMIN">Quản trị viên</option>
                            </select>
                          )}
                        </td>
                      )}
                      <td className="py-3 px-4 text-right">
                        {canRemove ? (
                          <button
                            type="button"
                            onClick={() => setMemberToRemove(member)}
                            className="inline-flex items-center text-xs font-medium text-[#C9372C] hover:text-[#AE2A19] hover:underline cursor-pointer"
                          >
                            {isSelf ? "Rời nhóm" : "Xóa khỏi nhóm"}
                          </button>
                        ) : (
                          <span className="text-[11px] text-[#626F86] italic">
                            —
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalMembers > pageSize && (
          <Pagination
            currentPage={currentPage}
            totalItems={totalMembers}
            pageSize={pageSize}
            onPageChange={(page) => setCurrentPage(page)}
            onPageSizeChange={(size) => setPageSize(size)}
          />
        )}
      </div>

      {/* Modal Thêm thành viên */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Thêm thành viên vào nhóm"
      >
        <div className="space-y-4">
          {modalError && (
            <Alert type="error" onClose={() => setModalError(null)}>
              {modalError}
            </Alert>
          )}

          <div>
            <label className="block text-xs font-semibold text-[#172B4D] mb-1">
              Tìm kiếm tài khoản
            </label>
            <Input
              placeholder="Nhập tên đăng nhập, email hoặc họ tên..."
              value={searchQuery}
              onChange={(e) => handleSearchCandidates(e.target.value)}
              autoFocus
            />
          </div>

          <div className="max-h-60 overflow-y-auto divide-y divide-[#DCDFE4] border border-[#DCDFE4] rounded">
            {isSearching ? (
              <div className="p-4 text-center text-xs text-[#626F86]">
                Đang tìm kiếm...
              </div>
            ) : candidates.length === 0 ? (
              <div className="p-4 text-center text-xs text-[#626F86]">
                Không tìm thấy người dùng phù hợp hoặc tất cả đã là thành viên.
              </div>
            ) : (
              candidates.map((c) => (
                <div
                  key={c.id}
                  className="flex items-center justify-between p-3 hover:bg-[#FAFBFC] transition-colors"
                >
                  <div>
                    <div className="text-xs font-semibold text-[#172B4D]">
                      {c.fullname || c.username}
                    </div>
                    <div className="text-[11px] text-[#626F86]">
                      @{c.username} • {c.email}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    onClick={() => handleAddMember(c.id)}
                    isLoading={addingUserId === c.id}
                    disabled={addingUserId !== null}
                  >
                    Thêm
                  </Button>
                </div>
              ))
            )}
          </div>

          <div className="flex justify-end pt-3 border-t border-[#EBECF0]">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsAddModalOpen(false)}
            >
              Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal xác nhận Xóa / Rời nhóm */}
      <Modal
        isOpen={memberToRemove !== null}
        onClose={() => setMemberToRemove(null)}
        title={
          memberToRemove?.username === currentLoggedUser?.username
            ? "Xác nhận rời khỏi nhóm"
            : "Xác nhận xóa thành viên"
        }
        confirmLabel={
          memberToRemove?.username === currentLoggedUser?.username
            ? "Rời nhóm"
            : "Xóa thành viên"
        }
        confirmVariant="danger"
        isConfirmLoading={isRemoving}
        onConfirm={handleConfirmRemove}
      >
        {memberToRemove?.username === currentLoggedUser?.username ? (
          <p className="text-xs text-[#44546F]">
            Bạn có chắc chắn muốn <strong>rời khỏi nhóm làm việc này</strong> không? Bạn sẽ không còn quyền truy cập dữ liệu của nhóm sau khi rời đi.
          </p>
        ) : (
          <p className="text-xs text-[#44546F]">
            Bạn có chắc chắn muốn xóa thành viên{" "}
            <strong>@{memberToRemove?.username}</strong> ({memberToRemove?.fullname || memberToRemove?.email}) ra khỏi nhóm làm việc này không?
          </p>
        )}
      </Modal>
    </div>
  );
}
