"use client";

import { ReactNode, useEffect } from "react";
import { Button } from "./button";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  confirmLabel?: string;
  confirmVariant?: "primary" | "danger";
  isConfirmLoading?: boolean;
  onConfirm?: () => void;
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  confirmLabel = "Xác nhận",
  confirmVariant = "primary",
  isConfirmLoading = false,
  onConfirm,
}: ModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen && !isConfirmLoading) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, isConfirmLoading, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#091E42]/60 animate-in fade-in duration-150">
      <div
        className="w-full max-w-md rounded-md bg-white p-5 shadow-lg border border-[#DCDFE4] transition-all scale-100"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div className="flex items-center justify-between border-b border-[#DCDFE4] pb-3">
          <h3 id="modal-title" className="text-base font-semibold text-[#172B4D]">
            {title}
          </h3>
          <button
            type="button"
            disabled={isConfirmLoading}
            onClick={onClose}
            className="p-1 rounded text-[#626F86] hover:text-[#172B4D] hover:bg-[#091E420F] cursor-pointer disabled:opacity-50 transition-colors"
            aria-label="Đóng"
          >
            <svg className="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </button>
        </div>

        <div className="py-4 text-sm text-[#44546F] leading-relaxed">{children}</div>

        <div className="flex items-center justify-end gap-2 pt-3 border-t border-[#DCDFE4]">
          <Button
            type="button"
            variant="secondary"
            size="md"
            disabled={isConfirmLoading}
            onClick={onClose}
          >
            Hủy bỏ
          </Button>
          {onConfirm && (
            <Button
              type="button"
              variant={confirmVariant}
              size="md"
              isLoading={isConfirmLoading}
              onClick={onConfirm}
            >
              {confirmLabel}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
