"use client";

import { useEffect, useState, useMemo, useSyncExternalStore } from "react";
import Link from "next/link";
import { adminService } from "@/services/admin.service";
import { userService } from "@/services/user.service";
import { tokenStorage } from "@/lib/token-storage";
import { AdminUserResponse, UserStatus, Role } from "@/types";
import { StatusBadge, Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TableSkeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { Alert } from "@/components/ui/alert";
import { Modal } from "@/components/ui/modal";
import { Pagination } from "@/components/ui/pagination";
import { extractErrorMessage } from "@/lib/api-client";

type ActionType = "lock" | "unlock" | "disable" | "activate" | "delete";

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

export default function AdminUsersPage() {
  const isAdmin = useSyncExternalStore(
    subscribe,
    () => tokenStorage.isAdmin(),
    () => false
  );

  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isAccessDenied, setIsAccessDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Filter states
  const [filterUsername, setFilterUsername] = useState("");
  const [filterEmail, setFilterEmail] = useState("");
  const [filterRole, setFilterRole] = useState("");
  const [filterStatus, setFilterStatus] = useState<UserStatus | "">("");

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Action Modal states
  const [selectedUser, setSelectedUser] = useState<AdminUserResponse | null>(null);
  const [actionType, setActionType] = useState<ActionType | null>(null);
  const [isActionLoading, setIsActionLoading] = useState(false);

  useEffect(() => {
    // If roles haven't been synced from /me yet, fetch profile in background
    if (!isAdmin) {
      userService.getProfile().then((profile) => {
        if (profile?.roles) {
          const rolesArray: string[] = Array.isArray(profile.roles)
            ? profile.roles
                .map((r) => (typeof r === "string" ? r : r.roleName || r.name || ""))
                .filter(Boolean)
            : [];
          tokenStorage.setUserProfile({
            id: profile.id,
            username: profile.username,
            roles: rolesArray,
          });
        }
      }).catch(() => {
        // Ignored
      });
    }

    Promise.all([
      adminService.searchUsers({
        username: filterUsername,
        email: filterEmail,
        role: filterRole,
        status: filterStatus,
      }),
      adminService.getRoles().catch(() => []),
    ])
      .then(([usersData, rolesData]) => {
        setUsers(usersData || []);
        setRoles(rolesData || []);
        setCurrentPage(1);
        setIsAccessDenied(false);
      })
      .catch((err) => {
        const msg = extractErrorMessage(err, "Không thể tải danh sách người dùng.");
        if (err && typeof err === "object" && "status" in err && (err as { status: number }).status === 403) {
          setIsAccessDenied(true);
        } else {
          setError(msg);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [isAdmin, filterUsername, filterEmail, filterRole, filterStatus]);

  const handleManualRefresh = () => {
    setIsLoading(true);
    setError(null);
    Promise.all([
      adminService.searchUsers({
        username: filterUsername,
        email: filterEmail,
        role: filterRole,
        status: filterStatus,
      }),
      adminService.getRoles().catch(() => []),
    ])
      .then(([usersData, rolesData]) => {
        setUsers(usersData || []);
        setRoles(rolesData || []);
        setIsAccessDenied(false);
      })
      .catch((err) => {
        if (err && typeof err === "object" && "status" in err && (err as { status: number }).status === 403) {
          setIsAccessDenied(true);
        } else {
          setError(extractErrorMessage(err, "Không thể làm mới danh sách."));
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleManualRefresh();
  };

  const handleFilterReset = () => {
    setFilterUsername("");
    setFilterEmail("");
    setFilterRole("");
    setFilterStatus("");
  };

  // Client-side pagination slicing
  const paginatedUsers = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return users.slice(start, start + pageSize);
  }, [users, currentPage, pageSize]);

  const openActionModal = (user: AdminUserResponse, type: ActionType) => {
    setSelectedUser(user);
    setActionType(type);
  };

  const closeActionModal = () => {
    setSelectedUser(null);
    setActionType(null);
  };

  const handleConfirmAction = async () => {
    if (!selectedUser || !actionType) return;
    setIsActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      let updatedUser: AdminUserResponse | null = null;
      switch (actionType) {
        case "lock":
          updatedUser = await adminService.lockUser(selectedUser.id);
          setSuccessMessage(`Đã khóa tài khoản "${selectedUser.username}".`);
          break;
        case "unlock":
          updatedUser = await adminService.unlockUser(selectedUser.id);
          setSuccessMessage(`Đã mở khóa tài khoản "${selectedUser.username}".`);
          break;
        case "disable":
          updatedUser = await adminService.disableUser(selectedUser.id);
          setSuccessMessage(`Đã vô hiệu hóa tài khoản "${selectedUser.username}".`);
          break;
        case "activate":
          updatedUser = await adminService.activateUser(selectedUser.id);
          setSuccessMessage(`Đã kích hoạt lại tài khoản "${selectedUser.username}".`);
          break;
        case "delete":
          await adminService.deleteUser(selectedUser.id);
          setSuccessMessage(`Đã vô hiệu hóa/xóa tài khoản "${selectedUser.username}".`);
          break;
      }

      if (actionType === "delete") {
        setUsers((prev) => prev.filter((u) => u.id !== selectedUser.id));
      } else if (updatedUser) {
        setUsers((prev) => prev.map((u) => (u.id === selectedUser.id ? updatedUser! : u)));
      } else {
        handleManualRefresh();
      }

      closeActionModal();
    } catch (err) {
      setError(extractErrorMessage(err, "Thao tác trên tài khoản thất bại."));
    } finally {
      setIsActionLoading(false);
    }
  };

  if (isAccessDenied) {
    return (
      <div className="rounded border border-[#DCDFE4] bg-white p-8 text-center max-w-lg mx-auto">
        <div className="mx-auto mb-3 grid h-10 w-10 place-items-center rounded bg-[#FFEDE9] text-[#C9372C]">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h3 className="text-base font-bold text-[#172B4D]">Truy cập bị từ chối</h3>
        <p className="mt-1 text-xs text-[#626F86] leading-relaxed">
          Tài khoản hiện tại không có quyền Quản trị viên (ROLE_ADMIN) để thực hiện thao tác quản lý người dùng.
        </p>
        <Link
          href="/dashboard"
          className="mt-4 inline-flex items-center rounded bg-[#0C66E4] px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-[#0055CC]"
        >
          Quay lại Bảng điều khiển
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Breadcrumb & Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#DCDFE4] pb-3">
        <div>
          <div className="flex items-center gap-1.5 text-xs text-[#626F86] mb-1">
            <Link href="/dashboard" className="hover:text-[#0C66E4]">
              Hệ thống
            </Link>
            <span>/</span>
            <span className="text-[#172B4D] font-medium">Quản lý người dùng</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] leading-tight">
            Quản lý người dùng
          </h1>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Danh sách toàn bộ người dùng, tìm kiếm theo tiêu chí, phân quyền và điều khiển trạng thái
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleManualRefresh}
            disabled={isLoading}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {successMessage && (
        <Alert type="success" onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {error && (
        <Alert type="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Filter Toolbar (Jira Control Bar style) */}
      <div className="rounded border border-[#DCDFE4] bg-white p-3.5">
        <form onSubmit={handleFilterSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
          <Input
            placeholder="Tìm theo username..."
            value={filterUsername}
            onChange={(e) => setFilterUsername(e.target.value)}
          />

          <Input
            placeholder="Tìm theo email..."
            value={filterEmail}
            onChange={(e) => setFilterEmail(e.target.value)}
          />

          <div>
            <select
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
              className="w-full h-[38px] rounded border border-[#DCDFE4] bg-white px-3 text-xs text-[#172B4D] focus:outline-none focus:border-[#0C66E4] focus:ring-2 focus:ring-[#0C66E4]/20 cursor-pointer"
            >
              <option value="">Tất cả vai trò</option>
              {roles.map((r, i) => {
                const name = r.roleName || r.name || "";
                return (
                  <option key={i} value={name}>
                    {name}
                  </option>
                );
              })}
            </select>
          </div>

          <div>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as UserStatus | "")}
              className="w-full h-[38px] rounded border border-[#DCDFE4] bg-white px-3 text-xs text-[#172B4D] focus:outline-none focus:border-[#0C66E4] focus:ring-2 focus:ring-[#0C66E4]/20 cursor-pointer"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="LOCKED">Đã khóa (LOCKED)</option>
              <option value="DISABLED">Vô hiệu hóa (DISABLED)</option>
              <option value="PENDING">Chờ kích hoạt (PENDING)</option>
            </select>
          </div>

          <div className="sm:col-span-2 lg:col-span-4 flex items-center justify-between pt-2 border-t border-[#EBECF0]">
            <span className="text-xs text-[#626F86]">
              Tìm thấy <strong className="text-[#172B4D]">{users.length}</strong> kết quả
            </span>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleFilterReset}
              >
                Đặt lại
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="sm"
                isLoading={isLoading}
              >
                Áp dụng lọc
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Users Table */}
      <div className="rounded border border-[#DCDFE4] bg-white overflow-hidden">
        {isLoading ? (
          <TableSkeleton rows={5} cols={6} />
        ) : users.length === 0 ? (
          <EmptyState
            title="Không tìm thấy người dùng"
            description="Không có tài khoản nào phù hợp với điều kiện tìm kiếm và lọc của bạn."
            action={
              <Button variant="outline" size="sm" onClick={handleFilterReset}>
                Xóa bộ lọc
              </Button>
            }
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs sm:text-sm text-[#172B4D]">
                <thead className="bg-[#F7F8F9] text-[11px] font-bold text-[#626F86] uppercase tracking-wider border-b border-[#DCDFE4]">
                  <tr>
                    <th scope="col" className="px-3.5 py-2.5 w-16">ID</th>
                    <th scope="col" className="px-3.5 py-2.5">Người dùng</th>
                    <th scope="col" className="px-3.5 py-2.5">Vai trò</th>
                    <th scope="col" className="px-3.5 py-2.5">Trạng thái</th>
                    <th scope="col" className="px-3.5 py-2.5 text-center">Sai MK</th>
                    <th scope="col" className="px-3.5 py-2.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#EBECF0]">
                  {paginatedUsers.map((u) => {
                    const userRoles = u.roles?.map((r) => (typeof r === "string" ? r : r.name)) || [];

                    return (
                      <tr key={u.id} className="hover:bg-[#F7F8F9] transition-colors">
                        {/* ID */}
                        <td className="px-3.5 py-2.5 font-mono text-xs text-[#626F86]">
                          #{u.id}
                        </td>

                        {/* User Identity with Avatar */}
                        <td className="px-3.5 py-2.5">
                          <div className="flex items-center gap-2.5">
                            <div
                              className="grid h-7 w-7 place-items-center rounded-full bg-[#E9F2FF] text-[#0C66E4] text-[11px] font-bold border border-[#85B8FF] shrink-0"
                              title={u.username}
                            >
                              {u.username.slice(0, 2).toUpperCase()}
                            </div>
                            <div>
                              <div className="font-semibold text-[#172B4D] leading-tight">
                                {u.username}
                              </div>
                              <div className="text-xs text-[#626F86] mt-0.5">{u.email}</div>
                              {u.fullname && (
                                <div className="text-[11px] text-[#8590A2]">{u.fullname}</div>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* Roles */}
                        <td className="px-3.5 py-2.5">
                          <div className="flex flex-wrap gap-1">
                            {userRoles.map((r, i) => (
                              <Badge key={i} variant={r === "ROLE_ADMIN" ? "danger" : "default"}>
                                {r}
                              </Badge>
                            ))}
                          </div>
                        </td>

                        {/* Status */}
                        <td className="px-3.5 py-2.5 whitespace-nowrap">
                          <StatusBadge status={u.status} />
                          {u.lockUntill && (
                            <div className="text-[10px] text-[#B65C02] mt-0.5 font-mono">
                              Khóa đến: {u.lockUntill.replace("T", " ")}
                            </div>
                          )}
                        </td>

                        {/* Failed Attempts */}
                        <td className="px-3.5 py-2.5 text-center font-mono text-xs">
                          {u.failedLoginAttemps > 0 ? (
                            <span className="font-semibold text-[#C9372C]">{u.failedLoginAttemps}</span>
                          ) : (
                            <span className="text-[#8590A2]">0</span>
                          )}
                        </td>

                        {/* Actions */}
                        <td className="px-3.5 py-2.5 text-right whitespace-nowrap">
                          <div className="flex items-center justify-end gap-1">
                            <Link
                              href={`/admin/users/${u.id}`}
                              className="rounded px-2 py-1 text-xs font-semibold text-[#0C66E4] hover:bg-[#E9F2FF] transition-colors"
                            >
                              Chi tiết
                            </Link>

                            {/* Status Quick Actions */}
                            {u.status === "ACTIVE" && (
                              <button
                                type="button"
                                onClick={() => openActionModal(u, "lock")}
                                className="rounded px-1.5 py-1 text-xs font-medium text-[#B65C02] hover:bg-[#FFF3D6] cursor-pointer"
                              >
                                Khóa
                              </button>
                            )}

                            {u.status === "LOCKED" && (
                              <button
                                type="button"
                                onClick={() => openActionModal(u, "unlock")}
                                className="rounded px-1.5 py-1 text-xs font-medium text-[#1F845A] hover:bg-[#DCFFF1] cursor-pointer"
                              >
                                Mở khóa
                              </button>
                            )}

                            {u.status !== "DISABLED" ? (
                              <button
                                type="button"
                                onClick={() => openActionModal(u, "disable")}
                                className="rounded px-1.5 py-1 text-xs font-medium text-[#C9372C] hover:bg-[#FFEDE9] cursor-pointer"
                              >
                                Vô hiệu
                              </button>
                            ) : (
                              <button
                                type="button"
                                onClick={() => openActionModal(u, "activate")}
                                className="rounded px-1.5 py-1 text-xs font-medium text-[#1F845A] hover:bg-[#DCFFF1] cursor-pointer"
                              >
                                Kích hoạt
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <Pagination
              currentPage={currentPage}
              totalItems={users.length}
              pageSize={pageSize}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </>
        )}
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={!!selectedUser && !!actionType}
        onClose={closeActionModal}
        title={
          actionType === "lock"
            ? "Khóa tài khoản người dùng"
            : actionType === "unlock"
            ? "Mở khóa tài khoản người dùng"
            : actionType === "disable"
            ? "Vô hiệu hóa tài khoản người dùng"
            : actionType === "activate"
            ? "Kích hoạt lại tài khoản người dùng"
            : "Xóa tài khoản người dùng"
        }
        confirmLabel={
          actionType === "lock"
            ? "Khóa tài khoản"
            : actionType === "unlock"
            ? "Mở khóa"
            : actionType === "disable"
            ? "Vô hiệu hóa"
            : actionType === "activate"
            ? "Kích hoạt"
            : "Xác nhận xóa"
        }
        confirmVariant={
          actionType === "lock" || actionType === "disable" || actionType === "delete"
            ? "danger"
            : "primary"
        }
        isConfirmLoading={isActionLoading}
        onConfirm={handleConfirmAction}
      >
        {selectedUser && (
          <div className="space-y-2">
            <p>
              Bạn có chắc chắn muốn{" "}
              <strong className="text-[#172B4D]">
                {actionType === "lock"
                  ? "khóa"
                  : actionType === "unlock"
                  ? "mở khóa"
                  : actionType === "disable"
                  ? "vô hiệu hóa"
                  : actionType === "activate"
                  ? "kích hoạt lại"
                  : "xóa"}
              </strong>{" "}
              tài khoản <span className="font-semibold text-[#0C66E4]">{selectedUser.username}</span>?
            </p>
            {(actionType === "lock" || actionType === "disable") && (
              <p className="text-xs text-[#C9372C] bg-[#FFEDE9] p-2 rounded border border-[#FD9891]">
                Người dùng này sẽ bị thu hồi phiên làm việc và không thể đăng nhập vào hệ thống cho tới khi được mở khóa.
              </p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
