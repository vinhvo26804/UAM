"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore } from "react";
import { tokenStorage } from "@/lib/token-storage";
import { authService } from "@/services/auth.service";
import { Modal } from "@/components/ui/modal";
import { Badge } from "@/components/ui/badge";
import { JiraAvatar } from "@/components/jira/JiraIcons";
import CreateIssueModal from "@/components/issues/CreateIssueModal";

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
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

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
    <header className="sticky top-0 z-30 h-14 border-b border-[#091E4224] bg-white">
      <div className="flex h-full items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-3">
          {/* Mobile hamburger menu toggle */}
          {currentUser && onToggleMobileSidebar && (
            <button
              type="button"
              onClick={onToggleMobileSidebar}
              className="p-1.5 rounded text-[#44546F] hover:bg-[#091E420F] md:hidden cursor-pointer"
              aria-label="Mở danh mục điều hướng"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          )}

          {/* Logo brand */}
          <Link
            href={currentUser ? "/dashboard" : "/"}
            className="flex items-center gap-2.5 rounded focus:outline-hidden focus-visible:ring-2 focus-visible:ring-[#0C66E4] group"
          >
            <div className="grid h-7 w-7 place-items-center rounded bg-[#0C66E4] text-xs font-black text-white shadow-xs group-hover:bg-[#0055CC] transition-colors">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M11.53 2c0 2.4-1.97 4.35-4.4 4.35H3.5C2.67 6.35 2 7.02 2 7.85v3.62c0 .83.67 1.5 1.5 1.5h3.63c2.43 0 4.4 1.95 4.4 4.35V21c0 .83.67 1.5 1.5 1.5h3.47c.83 0 1.5-.67 1.5-1.5v-3.68c0-2.4 1.97-4.35 4.4-4.35h3.63c.83 0 1.5-.67 1.5-1.5V7.85c0-.83-.67-1.5-1.5-1.5h-3.63c-2.43 0-4.4-1.95-4.4-4.35V-0.5c0-.83-.67-1.5-1.5-1.5h-3.47c-.83 0-1.5.67-1.5 1.5V2z"/>
              </svg>
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-base font-bold tracking-tight text-[#172B4D] leading-none">
                Jira Clone
              </span>
              <span className="hidden sm:inline text-xs font-normal text-[#626F86]">
                Quản lý công việc
              </span>
            </div>
          </Link>

          {/* Jira-style Global Create Button */}
          {currentUser && (
            <button
              type="button"
              onClick={() => setIsCreateModalOpen(true)}
              className="ml-3 inline-flex items-center gap-1.5 rounded bg-[#0C66E4] hover:bg-[#0055CC] text-white px-3 py-1.5 text-xs font-semibold shadow-xs jira-interactive-btn cursor-pointer"
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
              </svg>
              <span>Tạo mới</span>
            </button>
          )}
        </div>

        {/* Right side navigation & user section */}
        <nav aria-label="Điều hướng chính" className="flex items-center gap-3 text-sm">
          {currentUser ? (
            <div className="flex items-center gap-3">
              <Link
                href="/spaces"
                className="hidden md:inline-flex items-center text-xs font-medium text-[#44546F] hover:text-[#0C66E4] transition-colors"
              >
                Không gian làm việc
              </Link>

              <div className="flex items-center gap-2.5 pl-3 border-l border-[#091E4224]">
                {/* User avatar badge */}
                <JiraAvatar name={currentUser.username} size="md" />

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
                  className="rounded border border-[#091E4224] bg-white px-2.5 py-1 text-xs font-medium text-[#44546F] hover:bg-[#FFEDE9] hover:text-[#C9372C] hover:border-[#FD9891] transition-colors cursor-pointer jira-interactive-btn"
                >
                  Đăng xuất
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                href="/login"
                className="rounded border border-[#091E4224] bg-white px-3 py-1.5 text-xs font-medium text-[#172B4D] hover:bg-[#F7F8F9] transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="rounded bg-[#0C66E4] px-3 py-1.5 text-xs font-medium text-white hover:bg-[#0055CC] transition-colors"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </nav>
      </div>

      {/* Global Create Issue Modal */}
      {isCreateModalOpen && (
        <CreateIssueModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
        />
      )}

      {/* Logout confirmation modal */}
      <Modal
        isOpen={isLogoutModalOpen}
        onClose={() => !isLoggingOut && setIsLogoutModalOpen(false)}
        title="Xác nhận đăng xuất"
      >
        <p className="text-xs text-[#44546F] mb-4">
          Bạn có chắc chắn muốn đăng xuất khỏi phiên làm việc hiện tại không?
        </p>
        <div className="flex justify-end gap-3 mt-4">
          <button
            type="button"
            disabled={isLoggingOut}
            onClick={() => setIsLogoutModalOpen(false)}
            className="px-3 py-1.5 text-xs text-[#44546F] hover:bg-[#091E420F] rounded font-medium cursor-pointer"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            disabled={isLoggingOut}
            onClick={handleLogout}
            className="px-3.5 py-1.5 text-xs bg-[#C9372C] hover:bg-[#AE2E24] text-white rounded font-medium disabled:opacity-50 cursor-pointer"
          >
            {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
          </button>
        </div>
      </Modal>
    </header>
  );
}
