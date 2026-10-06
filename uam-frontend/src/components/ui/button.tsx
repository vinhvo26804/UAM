import { ButtonHTMLAttributes, forwardRef, ReactNode } from "react";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger" | "outline" | "ghost" | "subtle";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = "primary",
      size = "md",
      isLoading = false,
      disabled,
      className = "",
      leftIcon,
      rightIcon,
      type = "button",
      ...props
    },
    ref
  ) => {
    const baseStyles =
      "inline-flex items-center justify-center font-medium rounded transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0C66E4] focus-visible:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer select-none";

    const sizeStyles = {
      sm: "h-8 px-2.5 text-xs gap-1.5",
      md: "h-9 px-3.5 text-sm gap-2",
      lg: "h-10 px-4 text-sm gap-2.5",
    }[size];

    const variantStyles = {
      primary: "bg-[#0C66E4] text-white hover:bg-[#0055CC] active:bg-[#09326C]",
      secondary: "bg-[#F1F2F4] text-[#172B4D] hover:bg-[#E9ECEF] active:bg-[#DCDFE4]",
      subtle: "bg-[#E9F2FF] text-[#0C66E4] hover:bg-[#CCE0FF] active:bg-[#B3D4FF]",
      danger: "bg-[#C9372C] text-white hover:bg-[#AE2E24] active:bg-[#5D1F1A]",
      outline: "border border-[#DCDFE4] text-[#172B4D] bg-white hover:bg-[#F7F8F9] active:bg-[#F1F2F4]",
      ghost: "text-[#44546F] hover:bg-[#091E420F] hover:text-[#172B4D] active:bg-[#091E4224]",
    }[variant];

    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled || isLoading}
        className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
        {...props}
      >
        {isLoading ? (
          <>
            <svg
              className="animate-spin -ml-0.5 h-3.5 w-3.5 text-current"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <span>Đang xử lý...</span>
          </>
        ) : (
          <>
            {leftIcon}
            {children}
            {rightIcon}
          </>
        )}
      </button>
    );
  }
);

Button.displayName = "Button";
