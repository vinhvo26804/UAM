import { ReactNode } from "react";

interface AlertProps {
  type?: "info" | "success" | "warning" | "error";
  title?: string;
  children: ReactNode;
  className?: string;
  onClose?: () => void;
}

export function Alert({
  type = "info",
  title,
  children,
  className = "",
  onClose,
}: AlertProps) {
  const styles = {
    info: {
      bg: "bg-[#E9F2FF] border-[#85B8FF] text-[#09326C]",
      iconColor: "text-[#0C66E4]",
      icon: (
        <svg className="w-4 h-4 text-[#0C66E4]" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
        </svg>
      ),
    },
    success: {
      bg: "bg-[#DCFFF1] border-[#7EE2B8] text-[#164B35]",
      iconColor: "text-[#1F845A]",
      icon: (
        <svg className="w-4 h-4 text-[#1F845A]" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
        </svg>
      ),
    },
    warning: {
      bg: "bg-[#FFF3D6] border-[#FEC57B] text-[#533F04]",
      iconColor: "text-[#B65C02]",
      icon: (
        <svg className="w-4 h-4 text-[#B65C02]" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
      ),
    },
    error: {
      bg: "bg-[#FFEDE9] border-[#FD9891] text-[#601E1A]",
      iconColor: "text-[#C9372C]",
      icon: (
        <svg className="w-4 h-4 text-[#C9372C]" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 00-1.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
        </svg>
      ),
    },
  }[type];

  return (
    <div
      role="alert"
      className={`flex items-start gap-2.5 rounded border px-3.5 py-2.5 text-xs sm:text-sm ${styles.bg} ${className}`}
    >
      <div className="shrink-0 mt-0.5">{styles.icon}</div>
      <div className="flex-1">
        {title && <h5 className="font-semibold mb-0.5 leading-snug">{title}</h5>}
        <div className="leading-relaxed">{children}</div>
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="shrink-0 -mr-1 -mt-1 p-1 rounded text-[#626F86] hover:text-[#172B4D] hover:bg-black/5 focus:outline-none cursor-pointer"
          aria-label="Đóng"
        >
          <svg className="w-3.5 h-3.5" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
          </svg>
        </button>
      )}
    </div>
  );
}
