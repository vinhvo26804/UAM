"use client";

import { useState, ReactNode } from "react";
import { Sidebar } from "@/components/sidebar";

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  return (
    <div className="flex min-h-[calc(100vh-56px)] bg-[#F7F8F9]">
      {/* Mobile float button to toggle sidebar */}
      <div className="md:hidden fixed bottom-4 right-4 z-30">
        <button
          type="button"
          onClick={() => setIsMobileOpen(true)}
          className="flex items-center gap-2 rounded-full bg-[#0C66E4] px-3.5 py-2 text-xs font-semibold text-white shadow-md hover:bg-[#0055CC] cursor-pointer"
          aria-label="Mở menu điều hướng"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          <span>Menu</span>
        </button>
      </div>

      <Sidebar isMobileOpen={isMobileOpen} onMobileClose={() => setIsMobileOpen(false)} />

      <main id="main-content" className="flex-1 min-w-0 p-4 sm:p-6 lg:p-7">
        <div className="w-full mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
}
