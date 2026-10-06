"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore } from "react";
import { tokenStorage } from "@/lib/token-storage";
import { authService } from "@/services/auth.service";
import { Modal } from "@/components/ui/modal";
import { Badge } from "@/components/ui/badge";

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}
type CurrentUser = ReturnType<typeof tokenStorage.getCurrentUser>;

let cachedUser: CurrentUser | null = null;
let cachedUserJson: string | null = null;

function getUserSnapshot(): CurrentUser | null {
  if (typeof window === "undefined") {
    return null;
  }

  const user = tokenStorage.getCurrentUser();
  const userJson = user == null ? null : JSON.stringify(user);

  if (userJson !== cachedUserJson) {
    cachedUserJson = userJson;
    cachedUser = user;
  }

  return cachedUser;
}

function getServerSnapshot(): null {
  return null;
}

export function Header({ onToggleMobileSidebar }: { onToggleMobileSidebar?: () => void }) {
  const router = useRouter();
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const currentUser = useSyncExternalStore(
    subscribe,
    getUserSnapshot,
    getServerSnapshot
  );

  const handleLogout = async () => {
    setIsLoggingOut(true);
    try {
      await authService.logout();
    } catch {
      tokenStorage.clearTokens();
    } finally {
      setIsLoggingOut(false);
      setIsLogoutModalOpen(false);
      router.push("/login");
    }
  };

  const isAdmin = currentUser?.roles.some((r) => r === "ROLE_ADMIN" || r === "ADMIN");

  return (
    <header className="sticky top-0 z-30 h-14 border-b border-[#DCDFE4] bg-white">
      <div className="flex h-full items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-3">
          {/* Mobile hamburger menu toggle */}
          {currentUser && onToggleMobileSidebar && (
            <button
              type="button"
              onClick={onToggleMobileSidebar}
              className="p-1.5 rounded text-[#44546F] hover:bg-[#F1F2F4] md:hidden cursor-pointer"
              aria-label="Mở danh mục điều hướng"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          )}

          {/* Logo brand */}
          <Link
            href="/"
            className="flex items-center gap-2.5 rounded focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0C66E4]"
          >
            <div className="grid h-7 w-7 place-items-center rounded bg-[#0C66E4] text-xs font-bold text-white">
              U
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-base font-bold tracking-tight text-[#172B4D] leading-none">
                UAM
              </span>
              <span className="hidden sm:inline text-xs font-normal text-[#626F86]">
                Quản lý tài khoản
              </span>
            </div>
          </Link>
        </div>

        {/* Right side navigation & user section */}
        <nav aria-label="Điều hướng chính" className="flex items-center gap-3 text-sm">
          {currentUser ? (
            <div className="flex items-center gap-3">
              <Link
                href="/dashboard"
                className="hidden md:inline-flex items-center text-xs font-medium text-[#44546F] hover:text-[#0C66E4] transition-colors"
              >
                Không gian làm việc
              </Link>

              <div className="flex items-center gap-2.5 pl-3 border-l border-[#DCDFE4]">
                {/* User avatar badge */}
                <div
                  className="grid h-7 w-7 place-items-center rounded-full bg-[#E9F2FF] text-[#0C66E4] text-xs font-semibold select-none border border-[#85B8FF]"
                  title={currentUser.username}
                >
                  {currentUser.username.slice(0, 2).toUpperCase()}
                </div>

                <div className="hidden sm:block text-left">
                  <div className="text-xs font-semibold text-[#172B4D] leading-tight">
                    {currentUser.username}
                  </div>
                  <div className="mt-0.5 leading-none">
                    {isAdmin ? (
                      <Badge variant="danger" size="sm">Admin</Badge>
                    ) : (
                      <Badge variant="primary" size="sm">Thành viên</Badge>
                    )}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setIsLogoutModalOpen(true)}
                  className="rounded border border-[#DCDFE4] bg-white px-2.5 py-1 text-xs font-medium text-[#44546F] hover:bg-[#FFEDE9] hover:text-[#C9372C] hover:border-[#FD9891] transition-colors cursor-pointer"
                >
                  Đăng xuất
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                href="/login"
                className="rounded bg-[#0C66E4] px-3 py-1.5 text-xs font-medium text-white hover:bg-[#0055CC] transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="rounded border border-[#DCDFE4] bg-white px-3 py-1.5 text-xs font-medium text-[#172B4D] hover:bg-[#F7F8F9] transition-colors"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </nav>
      </div>

      <Modal
        isOpen={isLogoutModalOpen}
        onClose={() => setIsLogoutModalOpen(false)}
        title="Xác nhận đăng xuất"
        confirmLabel="Đăng xuất"
        confirmVariant="danger"
        isConfirmLoading={isLoggingOut}
        onConfirm={handleLogout}
      >
        Bạn có chắc chắn muốn đăng xuất khỏi hệ thống không? Phiên làm việc hiện tại của bạn sẽ kết thúc.
      </Modal>
    </header>
  );
}
