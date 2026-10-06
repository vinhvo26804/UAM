"use client";

import { useState, FormEvent, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Alert } from "@/components/ui/alert";
import { extractErrorMessage } from "@/lib/api-client";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const initialInfoMessage = (() => {
    if (searchParams.get("session_expired")) {
      return "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.";
    }
    if (searchParams.get("registered")) {
      return "Đăng ký thành công và đã kích hoạt. Bạn có thể đăng nhập ngay.";
    }
    return null;
  })();

  const [infoMessage, setInfoMessage] = useState<string | null>(initialInfoMessage);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!username.trim()) {
      setErrorMessage("Vui lòng nhập tên đăng nhập.");
      return;
    }
    if (!password) {
      setErrorMessage("Vui lòng nhập mật khẩu.");
      return;
    }

    setIsLoading(true);
    try {
      await authService.login({
        userName: username.trim(),
        password: password,
      });

      router.push("/dashboard");
    } catch (err) {
      const msg = extractErrorMessage(err, "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="rounded-md border border-[#DCDFE4] bg-white p-6 sm:p-7 shadow-xs">
      {/* Brand & Title */}
      <div className="text-center mb-5">
        <div className="mx-auto mb-2.5 grid h-8 w-8 place-items-center rounded bg-[#0C66E4] text-xs font-bold text-white">
          U
        </div>
        <h1 className="text-lg sm:text-xl font-bold text-[#172B4D]">Đăng nhập UAM</h1>
        <p className="mt-0.5 text-xs text-[#626F86]">
          Nhập tài khoản để truy cập không gian quản lý
        </p>
      </div>

      {infoMessage && (
        <Alert type="info" className="mb-4" onClose={() => setInfoMessage(null)}>
          {infoMessage}
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
          id="username"
          type="text"
          placeholder="Nhập username"
          required
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          disabled={isLoading}
        />

        <div>
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-semibold text-[#44546F]">
              Mật khẩu <span className="text-[#C9372C]">*</span>
            </span>
            <Link
              href="/forgot-password"
              className="text-xs font-medium text-[#0C66E4] hover:underline"
            >
              Quên mật khẩu?
            </Link>
          </div>
          <PasswordInput
            id="password"
            placeholder="Nhập mật khẩu"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="md"
          className="w-full mt-2"
          isLoading={isLoading}
        >
          Đăng nhập
        </Button>
      </form>

      <div className="mt-5 pt-4 border-t border-[#EBECF0] text-center text-xs text-[#626F86]">
        Chưa có tài khoản?{" "}
        <Link href="/register" className="font-semibold text-[#0C66E4] hover:underline">
          Đăng ký tài khoản mới
        </Link>
      </div>

      <div className="mt-2 text-center text-[11px] text-[#8590A2]">
        Đã nhận mã kích hoạt?{" "}
        <Link href="/verify-otp" className="text-[#44546F] hover:text-[#0C66E4] underline">
          Xác thực OTP tại đây
        </Link>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="p-8 text-center text-xs text-[#626F86]">Đang tải biểu mẫu...</div>}>
      <LoginForm />
    </Suspense>
  );
}
