import { forwardRef, InputHTMLAttributes, ReactNode } from "react";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, leftIcon, className = "", id, ...props }, ref) => {
    const inputId = id || props.name || Math.random().toString(36).substring(7);

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-xs font-semibold text-[#44546F] mb-1.5"
          >
            {label}
            {props.required && <span className="text-[#C9372C] ml-1">*</span>}
          </label>
        )}
        <div className="relative rounded">
          {leftIcon && (
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-[#626F86]">
              {leftIcon}
            </div>
          )}
          <input
            ref={ref}
            id={inputId}
            className={`w-full rounded border bg-white px-3 py-2 text-sm text-[#172B4D] transition-colors placeholder:text-[#8590A2] focus:outline-none focus:ring-2 disabled:bg-[#F1F2F4] disabled:text-[#8590A2] disabled:cursor-not-allowed ${
              leftIcon ? "pl-9" : ""
            } ${
              error
                ? "border-[#C9372C] focus:border-[#C9372C] focus:ring-[#C9372C]/20"
                : "border-[#DCDFE4] focus:border-[#0C66E4] focus:ring-[#0C66E4]/20 hover:border-[#8590A2]"
            } ${className}`}
            {...props}
          />
        </div>
        {error && <p className="mt-1 text-xs text-[#C9372C]">{error}</p>}
        {!error && helperText && <p className="mt-1 text-xs text-[#626F86]">{helperText}</p>}
      </div>
    );
  }
);

Input.displayName = "Input";
