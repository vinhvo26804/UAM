"use client";

import { useEffect, useState, FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { adminService } from "@/services/admin.service";
import { AdminUserResponse, Role } from "@/types";
import { StatusBadge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert } from "@/components/ui/alert";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import { extractErrorMessage } from "@/lib/api-client";

export default function AdminUserDetailPage() {
  const params = useParams();
  const router = useRouter();
  const userId = Number(params?.id);

  const [user, setUser] = useState<AdminUserResponse | null>(null);
  const [availableRoles, setAvailableRoles] = useState<Role[]>([]);
  const [selectedRoleToAssign, setSelectedRoleToAssign] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  // Form states
  const [email, setEmail] = useState("");
  const [fullname, setFullname] = useState("");
  const [phone, setPhone] = useState("");
  const [dob, setDob] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);

  // Feedback states
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Status Action Modal states
  const [pendingAction, setPendingAction] = useState<"lock" | "unlock" | "disable" | "activate" | "delete" | null>(null);
  const [isActionLoading, setIsActionLoading] = useState(false);

  // Role Action states
  const [roleToRemove, setRoleToRemove] = useState<string | null>(null);
  const [isRoleActionLoading, setIsRoleActionLoading] = useState(false);

  useEffect(() => {
    if (!userId || isNaN(userId)) {
      return;
    }

    Promise.all([
      adminService.getUserById(userId),
      adminService.getRoles().catch(() => []),
    ])
      .then(([userData, rolesData]) => {
        setUser(userData);
        setEmail(userData.email || "");
        setFullname(userData.fullname || "");
        setPhone(userData.phone || "");
        setDob(userData.doB || userData.dob || "");
        setAvailableRoles(rolesData);
      })
      .catch((err) => {
        setErrorMessage(extractErrorMessage(err, "Không thể tải thông tin người dùng."));
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [userId]);

  const handleUpdateInfo = async (e: FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setIsUpdating(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      const updated = await adminService.updateUser(userId, {
        username: user.username,
        email: email.trim() || undefined,
        fullname: fullname.trim() || undefined,
        phone: phone.trim() || undefined,
        doB: dob || undefined,
      });

      setUser(updated);
      setSuccessMessage("Cập nhật thông tin người dùng thành công!");
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "Cập nhật thông tin thất bại."));
    } finally {
      setIsUpdating(false);
    }
  };

  const handleConfirmStatusAction = async () => {
    if (!user || !pendingAction) return;
    setIsActionLoading(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      let updated: AdminUserResponse | null = null;
      switch (pendingAction) {
        case "lock":
          updated = await adminService.lockUser(userId);
          setSuccessMessage("Đã khóa tài khoản người dùng.");
          break;
        case "unlock":
          updated = await adminService.unlockUser(userId);
          setSuccessMessage("Đã mở khóa tài khoản người dùng.");
          break;
        case "disable":
          updated = await adminService.disableUser(userId);
          setSuccessMessage("Đã vô hiệu hóa tài khoản người dùng.");
          break;
        case "activate":
          updated = await adminService.activateUser(userId);
          setSuccessMessage("Đã kích hoạt lại tài khoản người dùng.");
          break;
        case "delete":
          await adminService.deleteUser(userId);
          setSuccessMessage("Đã vô hiệu hóa/xóa tài khoản.");
          setTimeout(() => router.push("/admin/users"), 1200);
          return;
      }

      if (updated) {
        setUser(updated);
      }
      setPendingAction(null);
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "Thao tác thay đổi trạng thái thất bại."));
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleAssignRole = async (e: FormEvent) => {
    e.preventDefault();
    if (!user || !selectedRoleToAssign) return;
    setIsRoleActionLoading(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      await adminService.assignRole({
        userName: user.username,
        roleName: selectedRoleToAssign,
      });

      setSuccessMessage(`Đã gán vai trò "${selectedRoleToAssign}" cho người dùng.`);
      const updated = await adminService.getUserById(userId);
      setUser(updated);
      setSelectedRoleToAssign("");
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "Gán vai trò thất bại."));
    } finally {
      setIsRoleActionLoading(false);
    }
  };

  const handleConfirmRemoveRole = async () => {
    if (!user || !roleToRemove) return;
    if (roleToRemove === "ROLE_USER" || roleToRemove === "USER") {
      setErrorMessage("Không thể gỡ vai trò cơ sở ROLE_USER của người dùng.");
      setRoleToRemove(null);
      return;
    }

    setIsRoleActionLoading(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      await adminService.removeRole({
        userName: user.username,
        roleName: roleToRemove,
      });

      setSuccessMessage(`Đã gỡ vai trò "${roleToRemove}" khỏi người dùng.`);
      const updated = await adminService.getUserById(userId);
      setUser(updated);
      setRoleToRemove(null);
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "Gỡ vai trò thất bại."));
    } finally {
      setIsRoleActionLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-48 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="p-6 text-center">
        <Alert type="error" className="mb-4">
          {errorMessage || "Không tìm thấy người dùng yêu cầu."}
        </Alert>
        <Link href="/admin/users" className="text-xs font-semibold text-[#0C66E4] hover:underline">
          &larr; Quay lại danh sách quản lý
        </Link>
      </div>
    );
  }

  const userRoles = user.roles?.map((r) => (typeof r === "string" ? r : r.name || r.roleName || "")) || [];
  const unassignedRoles = availableRoles.filter(
    (ar) => {
      const name = ar.roleName || ar.name || "";
      return name && !userRoles.includes(name);
    }
  );

  return (
    <div className="space-y-5">
      {/* Breadcrumb & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#DCDFE4] pb-3">
        <div>
          <div className="flex items-center gap-1.5 text-xs text-[#626F86] mb-1">
            <Link href="/admin/users" className="hover:text-[#0C66E4]">
              Quản lý người dùng
            </Link>
            <span>/</span>
            <span className="font-mono text-[#172B4D] font-medium">#{user.id}</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="grid h-9 w-9 place-items-center rounded-full bg-[#E9F2FF] text-[#0C66E4] text-xs font-bold border border-[#85B8FF]">
              {user.username.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg sm:text-xl font-bold text-[#172B4D]">
                  {user.username}
                </h1>
                <StatusBadge status={user.status} />
              </div>
              <p className="text-xs text-[#626F86]">{user.email}</p>
            </div>
          </div>
        </div>

        {/* Quick Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          {user.status === "ACTIVE" && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPendingAction("lock")}
            >
              Khóa tài khoản
            </Button>
          )}

          {user.status === "LOCKED" && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => setPendingAction("unlock")}
            >
              Mở khóa
            </Button>
          )}

          {user.status !== "DISABLED" ? (
            <Button
              variant="danger"
              size="sm"
              onClick={() => setPendingAction("disable")}
            >
              Vô hiệu hóa
            </Button>
          ) : (
            <Button
              variant="primary"
              size="sm"
              onClick={() => setPendingAction("activate")}
            >
              Kích hoạt lại
            </Button>
          )}
        </div>
      </div>

      {successMessage && (
        <Alert type="success" onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {errorMessage && (
        <Alert type="error" onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      {/* Account Metadata Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="rounded border border-[#DCDFE4] bg-white p-3">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block">
            ID Người dùng
          </span>
          <p className="mt-0.5 font-mono text-xs font-bold text-[#172B4D]">#{user.id}</p>
        </div>

        <div className="rounded border border-[#DCDFE4] bg-white p-3">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block">
            Lần đăng nhập sai
          </span>
          <p className="mt-0.5 text-xs font-bold text-[#172B4D]">
            {user.failedLoginAttemps > 0 ? (
              <span className="text-[#C9372C]">{user.failedLoginAttemps} lần</span>
            ) : (
              "0 lần"
            )}
          </p>
        </div>

        <div className="rounded border border-[#DCDFE4] bg-white p-3">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block">
            Thời hạn khóa
          </span>
          <p className="mt-0.5 text-xs font-mono text-[#172B4D]">
            {user.lockUntill ? user.lockUntill.replace("T", " ") : "Không áp dụng"}
          </p>
        </div>

        <div className="rounded border border-[#DCDFE4] bg-white p-3">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block">
            Ngày khởi tạo
          </span>
          <p className="mt-0.5 text-xs font-mono text-[#172B4D]">
            {user.createdAt ? user.createdAt.split("T")[0] : "—"}
          </p>
        </div>
      </div>

      {/* Edit Basic Info Form */}
      <div className="rounded border border-[#DCDFE4] bg-white p-4 sm:p-5">
        <div className="border-b border-[#EBECF0] pb-2.5 mb-4">
          <h2 className="text-sm font-bold text-[#172B4D]">Thông tin cá nhân</h2>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Cập nhật địa chỉ email, họ tên, số điện thoại và ngày sinh
          </p>
        </div>

        <form onSubmit={handleUpdateInfo} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <Input
              label="Tên đăng nhập"
              id="edit-username"
              type="text"
              value={user.username}
              disabled
              className="bg-[#F1F2F4] text-[#8590A2] cursor-not-allowed"
            />

            <Input
              label="Địa chỉ Email"
              id="edit-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isUpdating}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
            <Input
              label="Họ và tên"
              id="edit-fullname"
              type="text"
              placeholder="Nhập họ tên"
              value={fullname}
              onChange={(e) => setFullname(e.target.value)}
              disabled={isUpdating}
            />

            <Input
              label="Số điện thoại"
              id="edit-phone"
              type="tel"
              placeholder="Nhập SĐT"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              disabled={isUpdating}
            />

            <Input
              label="Ngày sinh"
              id="edit-dob"
              type="date"
              value={dob}
              onChange={(e) => setDob(e.target.value)}
              disabled={isUpdating}
            />
          </div>

          <div className="pt-2 flex justify-end">
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isUpdating}
            >
              Lưu cập nhật
            </Button>
          </div>
        </form>
      </div>

      {/* Role Management Card */}
      <div className="rounded border border-[#DCDFE4] bg-white p-4 sm:p-5">
        <div className="border-b border-[#EBECF0] pb-2.5 mb-4">
          <h2 className="text-sm font-bold text-[#172B4D]">Phân quyền & Vai trò</h2>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Quản lý danh sách vai trò hệ thống gán cho người dùng
          </p>
        </div>

        {/* Current Roles List */}
        <div className="mb-4">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block mb-2">
            Vai trò hiện có
          </span>
          <div className="flex flex-wrap gap-1.5">
            {userRoles.map((r, i) => (
              <div
                key={i}
                className="inline-flex items-center gap-1.5 rounded border border-[#DCDFE4] bg-[#F1F2F4] px-2.5 py-1 text-xs font-medium text-[#172B4D]"
              >
                <span>{r}</span>
                {r !== "ROLE_USER" && r !== "USER" && (
                  <button
                    type="button"
                    onClick={() => setRoleToRemove(r || null)}
                    className="text-[#626F86] hover:text-[#C9372C] transition-colors cursor-pointer"
                    title="Gỡ vai trò"
                  >
                    &times;
                  </button>
                )}
              </div>
            ))}
          </div>
          <p className="mt-2 text-[11px] text-[#626F86]">
            * Vai trò cơ bản <code>ROLE_USER</code> được gán mặc định và không thể thu hồi.
          </p>
        </div>

        {/* Assign New Role Form */}
        <form onSubmit={handleAssignRole} className="flex flex-wrap items-center gap-2.5 pt-3 border-t border-[#EBECF0]">
          <div className="w-64">
            <select
              value={selectedRoleToAssign}
              onChange={(e) => setSelectedRoleToAssign(e.target.value)}
              className="w-full h-8 rounded border border-[#DCDFE4] bg-white px-2.5 text-xs text-[#172B4D] focus:outline-none focus:border-[#0C66E4] cursor-pointer"
            >
              <option value="">-- Chọn vai trò để gán --</option>
              {unassignedRoles.map((r, i) => {
                const name = r.roleName || r.name || "";
                return (
                  <option key={i} value={name}>
                    {name} {r.description ? `(${r.description})` : ""}
                  </option>
                );
              })}
            </select>
          </div>

          <Button
            type="submit"
            variant="outline"
            size="sm"
            disabled={!selectedRoleToAssign}
            isLoading={isRoleActionLoading}
          >
            Gán vai trò
          </Button>
        </form>
      </div>

      {/* Confirmation Modal for Status Action */}
      <Modal
        isOpen={!!pendingAction}
        onClose={() => setPendingAction(null)}
        title={
          pendingAction === "lock"
            ? "Khóa tài khoản"
            : pendingAction === "unlock"
            ? "Mở khóa tài khoản"
            : pendingAction === "disable"
            ? "Vô hiệu hóa tài khoản"
            : pendingAction === "activate"
            ? "Kích hoạt lại tài khoản"
            : "Xóa tài khoản"
        }
        confirmLabel="Xác nhận thực hiện"
        confirmVariant={pendingAction === "unlock" || pendingAction === "activate" ? "primary" : "danger"}
        isConfirmLoading={isActionLoading}
        onConfirm={handleConfirmStatusAction}
      >
        Bạn có chắc chắn muốn thay đổi trạng thái tài khoản <strong className="text-[#172B4D]">{user.username}</strong> sang{" "}
        <strong className="text-[#C9372C]">{pendingAction?.toUpperCase()}</strong> không?
      </Modal>

      {/* Confirmation Modal for Removing Role */}
      <Modal
        isOpen={!!roleToRemove}
        onClose={() => setRoleToRemove(null)}
        title="Thu hồi vai trò"
        confirmLabel="Gỡ quyền"
        confirmVariant="danger"
        isConfirmLoading={isRoleActionLoading}
        onConfirm={handleConfirmRemoveRole}
      >
        Bạn có chắc chắn muốn thu hồi vai trò <strong className="text-[#172B4D]">{roleToRemove}</strong> khỏi tài khoản{" "}
        <strong className="text-[#172B4D]">{user.username}</strong> không?
      </Modal>
    </div>
  );
}
