import type { ReactNode } from "react";
import { UserStatus } from "@/types";

interface BadgeProps {
  children: ReactNode;
  variant?: "default" | "success" | "warning" | "danger" | "info" | "neutral" | "primary";
  size?: "sm" | "md";
}

export function Badge({ children, variant = "default", size = "sm" }: BadgeProps) {
  const sizeClasses = size === "sm" ? "px-1.5 py-0.5 text-xs leading-none" : "px-2 py-1 text-xs";

  const variantClasses = {
    default: "bg-[#F1F2F4] text-[#44546F] border-[#DCDFE4]",
    neutral: "bg-[#F1F2F4] text-[#44546F] border-[#DCDFE4]",
    primary: "bg-[#E9F2FF] text-[#0C66E4] border-[#85B8FF]",
    info: "bg-[#E9F2FF] text-[#0C66E4] border-[#85B8FF]",
    success: "bg-[#DCFFF1] text-[#1F845A] border-[#7EE2B8]",
    warning: "bg-[#FFF3D6] text-[#B65C02] border-[#FEC57B]",
    danger: "bg-[#FFEDE9] text-[#C9372C] border-[#FD9891]",
  }[variant];

  return (
    <span
      className={`inline-flex items-center font-medium rounded border ${sizeClasses} ${variantClasses}`}
    >
      {children}
    </span>
  );
}

export function StatusBadge({ status }: { status: UserStatus | string }) {
  switch (status) {
    case "ACTIVE":
      return <Badge variant="success">Hoạt động</Badge>;
    case "LOCKED":
      return <Badge variant="warning">Đã khóa</Badge>;
    case "DISABLED":
      return <Badge variant="danger">Vô hiệu hóa</Badge>;
    case "PENDING":
      return <Badge variant="info">Chờ kích hoạt</Badge>;
    default:
      return <Badge variant="neutral">{status}</Badge>;
  }
}
