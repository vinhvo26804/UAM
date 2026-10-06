"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { userService } from "@/services/user.service";
import { tokenStorage } from "@/lib/token-storage";
import { UserResponse } from "@/types";
import { StatusBadge, Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert } from "@/components/ui/alert";
import { extractErrorMessage } from "@/lib/api-client";

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = tokenStorage.getAccessToken();
    if (!token) {
      router.replace("/login");
      return;
    }

    async function loadProfile() {
      try {
        const data = await userService.getProfile();
        setUser(data);
      } catch (err) {
        setError(extractErrorMessage(err, "Không thể tải thông tin người dùng. Vui lòng đăng nhập lại."));
      } finally {
        setIsLoading(false);
      }
    }

    loadProfile();
  }, [router]);

  const isAdmin = tokenStorage.isAdmin();

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-20 w-full" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-4">
        <Alert type="error">{error}</Alert>
        <Link
          href="/login"
          className="inline-flex items-center px-3 py-1.5 rounded bg-[#0C66E4] text-white text-xs font-medium hover:bg-[#0055CC]"
        >
          Đăng nhập lại
        </Link>
      </div>
    );
  }

  const roleNames = user?.roles?.map((r) => (typeof r === "string" ? r : r.name || r.roleName || "UNKNOWN")) || [];

  return (
    <div className="space-y-5">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#DCDFE4] pb-3">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] leading-tight">
            Bảng điều khiển
          </h1>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Xem tóm tắt thông tin tài khoản và truy cập nhanh các chức năng hệ thống
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href="/profile"
            className="rounded border border-[#DCDFE4] bg-white px-3 py-1.5 text-xs font-medium text-[#172B4D] hover:bg-[#F7F8F9] transition-colors"
          >
            Hồ sơ cá nhân
          </Link>
          <Link
            href="/security"
            className="rounded bg-[#0C66E4] px-3 py-1.5 text-xs font-medium text-white hover:bg-[#0055CC] transition-colors"
          >
            Bảo mật
          </Link>
        </div>
      </div>

      {/* User Welcome Card */}
      <div className="rounded border border-[#DCDFE4] bg-white p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="grid h-12 w-12 place-items-center rounded-full bg-[#E9F2FF] text-[#0C66E4] text-lg font-bold border border-[#85B8FF] shrink-0">
              {user?.username ? user.username.slice(0, 2).toUpperCase() : "U"}
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base font-bold text-[#172B4D]">
                  {user?.fullname || user?.username}
                </h2>
                {user?.status && <StatusBadge status={user.status} />}
              </div>
              <p className="mt-0.5 text-xs text-[#626F86]">
                Tên đăng nhập: <strong className="text-[#172B4D]">{user?.username}</strong> &bull; ID: #{user?.id}
              </p>
            </div>
          </div>

          <div className="text-left sm:text-right text-xs text-[#626F86]">
            <span>Trạng thái kết nối:</span>{" "}
            <span className="inline-block w-2 h-2 rounded-full bg-[#1F845A] ml-1 mr-1" />
            <span className="font-medium text-[#1F845A]">Trực tuyến</span>
          </div>
        </div>
      </div>

      {/* Information Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Account Details */}
        <div className="rounded border border-[#DCDFE4] bg-white p-4">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block mb-2">
            Định danh người dùng
          </span>
          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-[#626F86]">ID hệ thống:</span>
              <span className="font-mono font-semibold text-[#172B4D]">#{user?.id}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#626F86]">Tên tài khoản:</span>
              <span className="font-semibold text-[#172B4D]">{user?.username}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#626F86]">Trạng thái:</span>
              <span>{user?.status ? <StatusBadge status={user.status} /> : "—"}</span>
            </div>
          </div>
        </div>

        {/* Roles Details */}
        <div className="rounded border border-[#DCDFE4] bg-white p-4">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block mb-2">
            Vai trò được cấp
          </span>
          <div className="flex flex-wrap gap-1.5 mt-1">
            {roleNames.length > 0 ? (
              roleNames.map((r, i) => (
                <Badge key={i} variant={r === "ROLE_ADMIN" ? "danger" : "default"}>
                  {r}
                </Badge>
              ))
            ) : (
              <Badge variant="default">ROLE_USER</Badge>
            )}
          </div>
          <p className="mt-3 text-[11px] text-[#626F86] leading-tight">
            * Quyền hạn trong hệ thống tương ứng với các vai trò được cấp trên.
          </p>
        </div>

        {/* Contact & Birthday */}
        <div className="rounded border border-[#DCDFE4] bg-white p-4">
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider block mb-2">
            Thông tin liên lạc
          </span>
          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-[#626F86]">Số điện thoại:</span>
              <span className="font-medium text-[#172B4D]">
                {user?.phone || <span className="text-[#8590A2]">Chưa cập nhật</span>}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#626F86]">Ngày sinh:</span>
              <span className="font-medium text-[#172B4D]">
                {user?.dob || user?.doB || <span className="text-[#8590A2]">Chưa cập nhật</span>}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Admin Fast Access Panel */}
      {isAdmin && (
        <div className="rounded border border-[#85B8FF] bg-[#E9F2FF] p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="grid h-5 w-5 place-items-center rounded bg-[#0C66E4] text-white text-[11px] font-bold">
                A
              </span>
              <h3 className="text-sm font-bold text-[#09326C]">
                Khu vực Quản trị viên (Admin Portal)
              </h3>
            </div>
            <p className="mt-1 text-xs text-[#172B4D] max-w-2xl leading-relaxed">
              Bạn có quyền Quản trị viên hệ thống. Nhấp vào nút bên cạnh để tra cứu, phân quyền, khóa hoặc quản lý người dùng.
            </p>
          </div>
          <Link
            href="/admin/users"
            className="rounded bg-[#0C66E4] px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-[#0055CC] transition-colors whitespace-nowrap shrink-0"
          >
            Quản lý người dùng &rarr;
          </Link>
        </div>
      )}
    </div>
  );
}
