"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useSyncExternalStore } from "react";
import { tokenStorage } from "@/lib/token-storage";
import { userService } from "@/services/user.service";

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

interface SidebarProps {
  isMobileOpen?: boolean;
  onMobileClose?: () => void;
}

export function Sidebar({ isMobileOpen = false, onMobileClose }: SidebarProps) {
  const pathname = usePathname();

  const isAdmin = useSyncExternalStore(
    subscribe,
    () => tokenStorage.isAdmin(),
    () => false
  );

  useEffect(() => {
    // If not identified as admin yet, try syncing profile in background if token exists
    const token = tokenStorage.getAccessToken();
    if (token && !isAdmin) {
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
  }, [isAdmin]);

  const baseItems = [
    {
      href: "/dashboard",
      label: "Tổng quan",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
      ),
    },
    {
      href: "/profile",
      label: "Hồ sơ cá nhân",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      ),
    },
    {
      href: "/security",
      label: "Bảo mật tài khoản",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
      ),
    },
  ];

  const adminItems = [
    {
      href: "/admin/users",
      label: "Quản lý người dùng",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
      ),
    },
  ];

  const renderNavLinks = (items: typeof baseItems) => (
    <div className="flex flex-col gap-0.5">
      {items.map((item) => {
        const isActive =
          pathname === item.href || (item.href !== "/dashboard" && pathname.startsWith(item.href));
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={() => onMobileClose && onMobileClose()}
            className={`flex items-center gap-2.5 rounded px-2.5 py-1.5 text-sm transition-colors ${
              isActive
                ? "bg-[#E9F2FF] text-[#0C66E4] font-semibold"
                : "text-[#44546F] hover:bg-[#091E420F] hover:text-[#172B4D]"
            }`}
          >
            <span className={isActive ? "text-[#0C66E4]" : "text-[#626F86]"}>{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        );
      })}
    </div>
  );

  const sidebarContent = (
    <div className="flex h-full flex-col p-3">
      {/* Personal Section */}
      <div className="mb-4">
        <p className="px-2.5 mb-1.5 text-[11px] font-bold tracking-wider text-[#626F86] uppercase">
          Tài khoản
        </p>
        <nav aria-label="Điều hướng cá nhân">{renderNavLinks(baseItems)}</nav>
      </div>

      {/* Admin Section */}
      {isAdmin && (
        <div className="pt-3 border-t border-[#DCDFE4]">
          <p className="px-2.5 mb-1.5 text-[11px] font-bold tracking-wider text-[#C9372C] uppercase">
            Quản trị viên
          </p>
          <nav aria-label="Điều hướng quản trị">{renderNavLinks(adminItems)}</nav>
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* Desktop Sidebar (fixed/sticky 240px) */}
      <aside className="hidden md:block w-[240px] shrink-0 border-r border-[#DCDFE4] bg-[#FAFBFC] min-h-[calc(100vh-56px)]">
        {sidebarContent}
      </aside>

      {/* Mobile Drawer */}
      {isMobileOpen && (
        <div className="fixed inset-0 z-40 md:hidden flex animate-in fade-in duration-150">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-[#091E42]/50"
            onClick={onMobileClose}
            aria-hidden="true"
          />

          {/* Drawer Body */}
          <div className="relative w-64 max-w-[80vw] bg-white h-full z-50 border-r border-[#DCDFE4] shadow-xl flex flex-col">
            <div className="flex items-center justify-between px-4 h-14 border-b border-[#DCDFE4]">
              <span className="text-sm font-bold text-[#172B4D]">Menu điều hướng</span>
              <button
                type="button"
                onClick={onMobileClose}
                className="p-1 rounded text-[#626F86] hover:bg-[#F1F2F4] cursor-pointer"
                aria-label="Đóng menu"
              >
                <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              {sidebarContent}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
