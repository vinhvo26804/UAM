import type { ReactNode } from "react";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main id="main-content" className="flex min-h-[calc(100vh-56px)] items-center justify-center bg-[#F7F8F9] px-4 py-8 sm:py-12">
      <div className="w-full max-w-[420px]">
        {children}
      </div>
    </main>
  );
}
