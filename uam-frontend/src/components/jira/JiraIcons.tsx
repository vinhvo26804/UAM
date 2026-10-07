import React from "react";
import { IssueType, IssuePriority, IssueStatus } from "@/types/issue";

interface IconProps {
  className?: string;
  size?: number;
}

// 1. Issue Type Icons (Jira Design System SVG icons)
export function IssueTypeIcon({
  type,
  className = "",
  size = 16,
}: {
  type: IssueType;
  className?: string;
  size?: number;
}) {
  switch (type) {
    case "EPIC":
      // Jira Epic: Purple rounded rect with lightning
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Epic"
        >
          <rect width="16" height="16" rx="3" fill="#904EE2" />
          <path
            d="M9.2 2.5L4.5 9H8L6.8 13.5L11.5 7H8L9.2 2.5Z"
            fill="white"
            stroke="white"
            strokeWidth="0.5"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "STORY":
      // Jira Story: Green rounded rect with bookmark / user story symbol
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Story"
        >
          <rect width="16" height="16" rx="3" fill="#00875A" />
          <path
            d="M4.5 3.5C4.5 3.22386 4.72386 3 5 3H11C11.2761 3 11.5 3.22386 11.5 3.5V12.5C11.5 12.8 11.15 12.98 10.9 12.8L8 10.75L5.1 12.8C4.85 12.98 4.5 12.8 4.5 12.5V3.5Z"
            fill="white"
          />
        </svg>
      );
    case "TASK":
      // Jira Task: Blue rounded rect with checkmark
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Task"
        >
          <rect width="16" height="16" rx="3" fill="#0052CC" />
          <path
            d="M5 8.2L7 10.2L11 6.2"
            stroke="white"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "BUG":
      // Jira Bug: Red rounded rect or circle with white dot / bug
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Bug"
        >
          <rect width="16" height="16" rx="3" fill="#DE350B" />
          <circle cx="8" cy="8" r="3.25" fill="white" />
        </svg>
      );
    case "SUBTASK":
      // Jira Subtask: Teal/slate rounded rect with branch
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Subtask"
        >
          <rect width="16" height="16" rx="3" fill="#00A3BF" />
          <path
            d="M4.5 4.5V10C4.5 10.8284 5.17157 11.5 6 11.5H11.5M11.5 11.5L9 9M11.5 11.5L9 14"
            stroke="white"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
        >
          <rect width="16" height="16" rx="3" fill="#626F86" />
        </svg>
      );
  }
}

// 2. Priority Icons (Jira standard icons)
export function PriorityIcon({
  priority,
  className = "",
  size = 14,
}: {
  priority: IssuePriority;
  className?: string;
  size?: number;
}) {
  switch (priority) {
    case "LOW":
      // Down green chevron
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Độ ưu tiên Thấp"
        >
          <path
            d="M4 6.5L8 10.5L12 6.5"
            stroke="#2ABB7F"
            strokeWidth="2.25"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "MEDIUM":
      // Equal horizontal amber bars
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Độ ưu tiên Trung bình"
        >
          <path
            d="M3.5 6.25H12.5M3.5 9.75H12.5"
            stroke="#FFAB00"
            strokeWidth="2.25"
            strokeLinecap="round"
          />
        </svg>
      );
    case "HIGH":
      // Up orange chevron
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Độ ưu tiên Cao"
        >
          <path
            d="M4 9.5L8 5.5L12 9.5"
            stroke="#FF5630"
            strokeWidth="2.25"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "CRITICAL":
      // Double up red chevrons
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 16 16"
          fill="none"
          className={`shrink-0 ${className}`}
          aria-label="Độ ưu tiên Khẩn cấp"
        >
          <path
            d="M4 7.5L8 3.5L12 7.5M4 12.5L8 8.5L12 12.5"
            stroke="#DE350B"
            strokeWidth="2.25"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      return null;
  }
}

// 3. Status Lozenge (Jira-style badge with text & accessible colors)
export function StatusLozenge({
  status,
  className = "",
  size = "md",
}: {
  status: IssueStatus;
  className?: string;
  size?: "sm" | "md";
}) {
  const sizeClasses = size === "sm" ? "px-1.5 py-0.5 text-[10px]" : "px-2 py-0.5 text-[11px]";

  switch (status) {
    case "TODO":
      return (
        <span
          className={`inline-flex items-center font-bold tracking-wide uppercase rounded ${sizeClasses} bg-[#DFE1E6] text-[#42526E] border border-[#C1C7D0] ${className}`}
        >
          CẦN LÀM
        </span>
      );
    case "IN_PROGRESS":
      return (
        <span
          className={`inline-flex items-center font-bold tracking-wide uppercase rounded ${sizeClasses} bg-[#DEEBFF] text-[#0747A6] border border-[#B3D4FF] ${className}`}
        >
          ĐANG LÀM
        </span>
      );
    case "DONE":
      return (
        <span
          className={`inline-flex items-center font-bold tracking-wide uppercase rounded ${sizeClasses} bg-[#E3FCEF] text-[#006644] border border-[#ABF5D1] ${className}`}
        >
          HOÀN THÀNH
        </span>
      );
    case "CANCELLED":
      return (
        <span
          className={`inline-flex items-center font-bold tracking-wide uppercase rounded ${sizeClasses} bg-[#FFEBE6] text-[#BF2600] border border-[#FFBDAD] ${className}`}
        >
          ĐÃ HỦY
        </span>
      );
    default:
      return (
        <span
          className={`inline-flex items-center font-bold tracking-wide uppercase rounded ${sizeClasses} bg-[#F4F5F7] text-[#626F86] ${className}`}
        >
          {status}
        </span>
      );
  }
}

// 4. Sprint & Backlog SVG Icons
export function SprintIcon({ className = "w-4 h-4" }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 16 16" fill="currentColor">
      <path d="M11.5 2C11.5 2.82843 10.8284 3.5 10 3.5C9.17157 3.5 8.5 2.82843 8.5 2C8.5 1.17157 9.17157 0.5 10 0.5C10.8284 0.5 11.5 1.17157 11.5 2Z" />
      <path d="M8.2 4.6L6.5 7.1L4.8 5.7L2.4 8.1L3.8 9.5L5.2 8.1L6.7 9.4L8.7 6.4L10.2 8.3L12.9 6.2L12 5L10.1 6.5L8.2 4.6Z" />
      <path d="M7 11.5L5.5 15.5H7.5L8.5 12.8L10 14.5V15.5H12V13.8L9.9 11.5H7Z" />
    </svg>
  );
}

export function BacklogIcon({ className = "w-4 h-4" }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 16 16" fill="none" stroke="currentColor">
      <rect x="2" y="2.5" width="12" height="11" rx="2" strokeWidth="1.5" />
      <line x1="2" y1="6.5" x2="14" y2="6.5" strokeWidth="1.5" />
      <line x1="6.5" y1="9.5" x2="9.5" y2="9.5" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

// 5. Jira User Avatar with Initials & deterministic color
const AVATAR_COLORS = [
  { bg: "#E9F2FF", text: "#0C66E4", border: "#85B8FF" },
  { bg: "#E3FCEF", text: "#006644", border: "#79F2C0" },
  { bg: "#EAE6FF", text: "#403294", border: "#B8ACF6" },
  { bg: "#FFF0B3", text: "#172B4D", border: "#FFE380" },
  { bg: "#FFEBE6", text: "#BF2600", border: "#FF8F73" },
  { bg: "#DEEBFF", text: "#0747A6", border: "#B3D4FF" },
];

export function JiraAvatar({
  name,
  size = "md",
  className = "",
}: {
  name?: string | null;
  size?: "xs" | "sm" | "md" | "lg";
  className?: string;
}) {
  const displayName = name?.trim() || "?";
  let hash = 0;
  for (let i = 0; i < displayName.length; i++) {
    hash = displayName.charCodeAt(i) + ((hash << 5) - hash);
  }
  const colorIndex = Math.abs(hash) % AVATAR_COLORS.length;
  const color = AVATAR_COLORS[colorIndex];

  const initials = displayName
    .split(/\s+/)
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  const sizeClasses = {
    xs: "w-4 h-4 text-[9px]",
    sm: "w-5 h-5 text-[10px]",
    md: "w-6 h-6 text-xs",
    lg: "w-8 h-8 text-sm",
  }[size];

  return (
    <div
      className={`inline-flex items-center justify-center font-bold rounded-full select-none shrink-0 border ${sizeClasses} ${className}`}
      style={{
        backgroundColor: color.bg,
        color: color.text,
        borderColor: color.border,
      }}
      title={displayName}
      aria-label={displayName}
    >
      {initials}
    </div>
  );
}
