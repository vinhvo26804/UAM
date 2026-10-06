"use client";

import { useState, FormEvent, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert } from "@/components/ui/alert";
import { extractErrorMessage } from "@/lib/api-client";

function VerifyOtpForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [username, setUsername] = useState(() => searchParams.get("username") || "");
  const [otpCode, setOtpCode] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!username.trim()) {
      setErrorMessage("Vui lòng nhập tên đăng nhập.");
      return;
    }
    if (!otpCode.trim() || otpCode.trim().length < 6) {
      setErrorMessage("Vui lòng nhập đủ 6 chữ số mã OTP.");
      return;
    }

    setIsLoading(true);
    try {
      await authService.verifyOtp({
        userName: username.trim(),
        otpCode: otpCode.trim(),
      });

      setSuccessMessage("Xác thực tài khoản thành công! Đang chuyển hướng về trang đăng nhập...");
      setTimeout(() => {
        router.push("/login?registered=1");
      }, 1500);
    } catch (err) {
      const msg = extractErrorMessage(err, "Mã OTP không chính xác hoặc đã hết hạn.");
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOtp = () => {
    setResendCooldown(60);
    setErrorMessage(
      "Tính năng gửi lại mã OTP đang được xử lý ở máy chủ. Vui lòng kiểm tra hộp thư email hoặc liên hệ Quản trị viên để được hỗ trợ cấp mã mới."
    );
  };

  return (
    <div className="rounded-md border border-[#DCDFE4] bg-white p-6 sm:p-7 shadow-xs">
      <div className="text-center mb-5">
        <div className="mx-auto mb-2.5 grid h-8 w-8 place-items-center rounded bg-[#E9F2FF] text-[#0C66E4] text-xs font-bold border border-[#85B8FF]">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h1 className="text-lg sm:text-xl font-bold text-[#172B4D]">Xác thực mã OTP</h1>
        <p className="mt-0.5 text-xs text-[#626F86]">
          Nhập mã 6 chữ số gửi qua email để kích hoạt tài khoản
        </p>
      </div>

      {successMessage && (
        <Alert type="success" className="mb-4">
          {successMessage}
        </Alert>
      )}

      {errorMessage && (
        <Alert type="error" className="mb-4" onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5">
        <Input
          label="Tên đăng nhập"
          id="otp-username"
          type="text"
          placeholder="Nhập tên tài khoản"
          required
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          disabled={isLoading}
        />

        <div>
          <Input
            label="Mã xác thực OTP (6 chữ số)"
            id="otp-code"
            type="text"
            maxLength={6}
            placeholder="Ví dụ: 123456"
            required
            className="tracking-widest text-center text-base font-mono"
            value={otpCode}
            onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
            disabled={isLoading}
          />
          <p className="mt-1 text-[11px] text-[#626F86]">Mã OTP có hiệu lực trong 5 phút từ khi đăng ký.</p>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="md"
          className="w-full mt-2"
          isLoading={isLoading}
        >
          Xác thực & Kích hoạt
        </Button>
      </form>

      <div className="mt-5 flex items-center justify-between text-xs text-[#626F86] pt-3.5 border-t border-[#EBECF0]">
        <button
          type="button"
          disabled={resendCooldown > 0}
          onClick={handleResendOtp}
          className="font-medium text-[#0C66E4] hover:underline disabled:text-[#8590A2] disabled:no-underline cursor-pointer disabled:cursor-not-allowed"
        >
          {resendCooldown > 0 ? `Gửi lại sau ${resendCooldown}s` : "Gửi lại mã"}
        </button>

        <Link href="/login" className="text-[#44546F] hover:text-[#0C66E4] hover:underline">
          Quay lại Đăng nhập
        </Link>
      </div>
    </div>
  );
}

export default function VerifyOtpPage() {
  return (
    <Suspense fallback={<div className="p-8 text-center text-xs text-[#626F86]">Đang tải...</div>}>
      <VerifyOtpForm />
    </Suspense>
  );
}
