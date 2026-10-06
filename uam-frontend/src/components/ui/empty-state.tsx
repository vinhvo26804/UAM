import { ReactNode } from "react";

export function EmptyState({
  title = "Không tìm thấy dữ liệu",
  description = "Hiện tại không có mục nào phù hợp với bộ lọc hoặc tìm kiếm của bạn.",
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center p-10 text-center bg-white">
      <div className="w-10 h-10 rounded bg-[#F1F2F4] flex items-center justify-center text-[#626F86] mb-2.5">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="currentColor"
          className="w-5 h-5"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5m8.25 3v6.75m0 0l-3-3m3 3l3-3M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z"
          />
        </svg>
      </div>
      <h4 className="text-sm font-semibold text-[#172B4D] mb-1">{title}</h4>
      <p className="text-xs text-[#626F86] max-w-sm mb-3.5 leading-relaxed">{description}</p>
      {action}
    </div>
  );
}
