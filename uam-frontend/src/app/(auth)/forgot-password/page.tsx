"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert } from "@/components/ui/alert";

export default function ForgotPasswordPage() {
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!usernameOrEmail.trim()) return;

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setStatusMessage(
        "Tính năng Quên mật khẩu qua email tự động đang được hoàn thiện ở máy chủ. Vui lòng liên hệ trực tiếp Quản trị viên để được hỗ trợ đặt lại mật khẩu nhanh chóng."
      );
    }, 500);
  };

  return (
    <div className="rounded-md border border-[#DCDFE4] bg-white p-6 sm:p-7 shadow-xs">
      <div className="text-center mb-5">
        <div className="mx-auto mb-2.5 grid h-8 w-8 place-items-center rounded bg-[#FFF3D6] text-[#B65C02] text-xs font-bold border border-[#FEC57B]">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.75}
            stroke="currentColor"
            className="w-4 h-4"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"
            />
          </svg>
        </div>
        <h1 className="text-lg sm:text-xl font-bold text-[#172B4D]">Khôi phục mật khẩu</h1>
        <p className="mt-0.5 text-xs text-[#626F86]">
          Nhập tài khoản hoặc email để gửi yêu cầu đặt lại mật khẩu
        </p>
      </div>

      {statusMessage && (
        <Alert type="warning" className="mb-4" onClose={() => setStatusMessage(null)}>
          {statusMessage}
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5">
        <Input
          label="Tên đăng nhập hoặc Email"
          id="forgot-identifier"
          type="text"
          placeholder="vd: nguyenvana hoặc email@example.com"
          required
          value={usernameOrEmail}
          onChange={(e) => setUsernameOrEmail(e.target.value)}
          disabled={isLoading}
        />

        <Button
          type="submit"
          variant="primary"
          size="md"
          className="w-full mt-2"
          isLoading={isLoading}
        >
          Gửi yêu cầu khôi phục
        </Button>
      </form>

      <div className="mt-5 pt-4 border-t border-[#EBECF0] text-center text-xs text-[#626F86]">
        Nhớ lại mật khẩu?{" "}
        <Link href="/login" className="font-semibold text-[#0C66E4] hover:underline">
          Đăng nhập ngay
        </Link>
      </div>
    </div>
  );
}
