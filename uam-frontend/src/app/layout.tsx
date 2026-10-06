import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Header } from "@/components/header";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "UAM | Quản lý tài khoản",
    template: "%s | UAM",
  },
  description: "Không gian quản lý tài khoản người dùng UAM.",
  icons: { icon: "/icon.svg" },
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="vi">
      <body className="min-h-screen antialiased">
        <a
          href="#main-content"
          className="sr-only rounded bg-white p-3 focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50"
        >
          Đến nội dung chính
        </a>
        <Header />
        {children}
      </body>
    </html>
  );
}
