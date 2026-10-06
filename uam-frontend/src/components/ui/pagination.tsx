"use client";

import { Button } from "./button";

interface PaginationProps {
  currentPage: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (size: number) => void;
  pageSizeOptions?: number[];
}

export function Pagination({
  currentPage,
  totalItems,
  pageSize,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 20, 50],
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const startItem = totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalItems);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-3 py-3 px-4 border-t border-[#DCDFE4] bg-white text-xs text-[#626F86]">
      <div className="flex items-center gap-3">
        <span>
          Đang xem <strong className="text-[#172B4D]">{startItem}</strong> -{" "}
          <strong className="text-[#172B4D]">{endItem}</strong> trong{" "}
          <strong className="text-[#172B4D]">{totalItems}</strong> người dùng
        </span>

        {onPageSizeChange && (
          <div className="flex items-center gap-1.5 ml-2 border-l border-[#DCDFE4] pl-3">
            <span className="text-[#626F86]">Hiển thị:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                onPageSizeChange(Number(e.target.value));
                onPageChange(1);
              }}
              className="h-7 rounded border border-[#DCDFE4] bg-white px-2 text-xs text-[#172B4D] focus:outline-none focus:border-[#0C66E4] cursor-pointer"
            >
              {pageSizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt} / trang
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1.5">
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage <= 1}
          onClick={() => onPageChange(currentPage - 1)}
        >
          Trang trước
        </Button>

        <span className="px-2.5 py-1 text-xs font-medium text-[#172B4D] bg-[#F1F2F4] rounded border border-[#DCDFE4]">
          {currentPage} / {totalPages}
        </span>

        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Trang sau
        </Button>
      </div>
    </div>
  );
}
