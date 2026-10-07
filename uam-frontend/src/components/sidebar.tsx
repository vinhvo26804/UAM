"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState, useSyncExternalStore, useCallback } from "react";
import { tokenStorage } from "@/lib/token-storage";
import { userService } from "@/services/user.service";
import { spaceService } from "@/services/space.service";
import { Space } from "@/types/issue";
import { extractErrorMessage } from "@/lib/api-client";

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

  const [isCollapsed, setIsCollapsed] = useState(false);
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [spacesLoading, setSpacesLoading] = useState(true);
  const [spacesError, setSpacesError] = useState<string | null>(null);

  const fetchRecentSpaces = useCallback(async () => {
    const token = tokenStorage.getAccessToken();
    if (!token) {
      setSpacesLoading(false);
      return;
    }
    setSpacesLoading(true);
    setSpacesError(null);
    try {
      const res = await spaceService.getMySpaces(0, 5);
      setSpaces(res.content || []);
    } catch (err) {
      setSpacesError(extractErrorMessage(err));
    } finally {
      setSpacesLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRecentSpaces();
  }, [fetchRecentSpaces]);

  useEffect(() => {
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
      }).catch(() => {});
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
      href: "/spaces",
      label: "Không gian làm việc",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      ),
    },
    {
      href: "/teams",
      label: "Nhóm làm việc",
      icon: (
        <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
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
            title={isCollapsed ? item.label : undefined}
            onClick={() => onMobileClose && onMobileClose()}
            className={`flex items-center gap-2.5 rounded px-2.5 py-1.5 text-xs font-medium transition-all group relative ${
              isActive
                ? "bg-[#E9F2FF] text-[#0C66E4] font-semibold shadow-xs"
                : "text-[#44546F] hover:bg-[#091E420F] hover:text-[#172B4D]"
            }`}
          >
            {isActive && (
              <span className="absolute left-0 top-1 bottom-1 w-0.5 bg-[#0C66E4] rounded-r" />
            )}
            <span className={`${isActive ? "text-[#0C66E4]" : "text-[#626F86] group-hover:text-[#172B4D]"}`}>
              {item.icon}
            </span>
            {!isCollapsed && <span className="truncate">{item.label}</span>}
          </Link>
        );
      })}
    </div>
  );

  const sidebarContent = (
    <div
      className={`flex flex-col h-full bg-[#F7F8F9] border-r border-[#091E4224] p-3 select-none transition-all duration-200 ease-in-out ${
        isCollapsed ? "w-16 items-center px-2" : "w-64"
      }`}
    >
      {/* Sidebar Header & Toggle */}
      <div className={`flex items-center pb-2 mb-1 ${isCollapsed ? "justify-center" : "justify-between px-2"}`}>
        {!isCollapsed && (
          <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider">
            Điều hướng
          </span>
        )}
        <button
          type="button"
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="p-1 rounded text-[#626F86] hover:text-[#172B4D] hover:bg-[#091E420F] transition-colors cursor-pointer hidden md:flex items-center justify-center"
          title={isCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
          aria-label={isCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
        >
          <svg
            className={`w-3.5 h-3.5 transition-transform duration-200 ${isCollapsed ? "rotate-180" : ""}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      </div>

      <nav className="flex-1 w-full space-y-4 overflow-y-auto">
        {renderNavLinks(baseItems)}

        {/* Section Spaces */}
        <div className="pt-3 border-t border-[#091E4214]">
          {!isCollapsed ? (
            <div className="px-2 py-1 mb-1 flex items-center justify-between">
              <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider">
                Không gian (Spaces)
              </span>
              <Link
                href="/spaces"
                onClick={() => onMobileClose && onMobileClose()}
                title="Quản lý và tạo Không gian"
                className="p-1 text-[#626F86] hover:text-[#0C66E4] hover:bg-[#091E420F] rounded cursor-pointer transition-colors"
              >
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
                </svg>
              </Link>
            </div>
          ) : (
            <div className="h-0.5 bg-[#091E4214] my-2" />
          )}

          {spacesLoading ? (
            <div className="px-2 py-2 space-y-2">
              <div className="h-5 bg-[#091E420F] rounded animate-pulse w-full" />
              <div className="h-5 bg-[#091E420F] rounded animate-pulse w-3/4" />
            </div>
          ) : spacesError ? (
            !isCollapsed && (
              <div className="px-2 py-1 text-xs text-[#DE350B]">
                <span>Không tải được spaces</span>
                <button
                  type="button"
                  onClick={fetchRecentSpaces}
                  className="ml-1 underline text-[#0C66E4] hover:text-[#0055CC] cursor-pointer"
                >
                  Thử lại
                </button>
              </div>
            )
          ) : spaces.length === 0 ? (
            !isCollapsed && (
              <div className="px-2 py-2 text-xs text-[#626F86] italic">
                Chưa có Không gian nào
              </div>
            )
          ) : (
            <div className="flex flex-col gap-0.5">
              {spaces.map((sp) => {
                const isSpaceActive = pathname.startsWith(`/spaces/${sp.id}`);
                return (
                  <Link
                    key={sp.id}
                    href={`/spaces/${sp.id}/issues`}
                    title={isCollapsed ? `${sp.key} - ${sp.name}` : undefined}
                    onClick={() => onMobileClose && onMobileClose()}
                    className={`flex items-center gap-2.5 rounded px-2 py-1.5 text-xs transition-colors group relative ${
                      isSpaceActive
                        ? "bg-[#E9F2FF] text-[#0C66E4] font-semibold"
                        : "text-[#44546F] hover:bg-[#091E420F] hover:text-[#172B4D]"
                    } ${isCollapsed ? "justify-center" : ""}`}
                  >
                    {isSpaceActive && (
                      <span className="absolute left-0 top-1 bottom-1 w-0.5 bg-[#0C66E4] rounded-r" />
                    )}
                    {/* Space square icon with key */}
                    <div
                      className={`w-5 h-5 rounded shrink-0 flex items-center justify-center text-[9px] font-bold transition-transform group-hover:scale-105 ${
                        isSpaceActive
                          ? "bg-[#0C66E4] text-white shadow-xs"
                          : "bg-[#091E4214] text-[#44546F] group-hover:bg-[#091E4224]"
                      }`}
                    >
                      {sp.key.slice(0, 2).toUpperCase()}
                    </div>
                    {!isCollapsed && <span className="truncate flex-1 font-medium">{sp.name}</span>}
                  </Link>
                );
              })}

              {!isCollapsed && (
                <Link
                  href="/spaces"
                  onClick={() => onMobileClose && onMobileClose()}
                  className="flex items-center gap-2 rounded px-2 py-1.5 text-xs text-[#626F86] hover:text-[#0C66E4] hover:bg-[#091E420F] transition-colors mt-0.5 font-medium"
                >
                  <svg className="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M12 5l7 7-7 7" />
                  </svg>
                  <span>Xem tất cả không gian</span>
                </Link>
              )}
            </div>
          )}
        </div>

        {isAdmin && (
          <div className="pt-3 border-t border-[#091E4214]">
            {!isCollapsed && (
              <div className="px-2 py-1 mb-1">
                <span className="text-[11px] font-bold text-[#626F86] uppercase tracking-wider">
                  Hệ thống
                </span>
              </div>
            )}
            {renderNavLinks(adminItems)}
          </div>
        )}
      </nav>
    </div>
  );

  return (
    <>
      {/* Desktop permanent sidebar */}
      <aside className="hidden md:block shrink-0 sticky top-14 h-[calc(100vh-56px)] z-20">
        {sidebarContent}
      </aside>

      {/* Mobile Drawer with smooth backdrop & slide */}
      {isMobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden animate-jira-fade">
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity"
            onClick={onMobileClose}
          />
          <div className="relative z-50 flex w-64 max-w-xs flex-col bg-white shadow-xl animate-jira-slide-in">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
