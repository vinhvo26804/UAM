"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Alert } from "@/components/ui/alert";
import { extractErrorMessage } from "@/lib/api-client";

export default function RegisterPage() {
  const router = useRouter();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [dob, setDob] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!username.trim()) {
      setErrorMessage("Vui lòng nhập tên đăng nhập.");
      return;
    }
    if (!email.trim() || !email.includes("@")) {
      setErrorMessage("Vui lòng nhập địa chỉ email hợp lệ.");
      return;
    }
    if (password.length < 8) {
      setErrorMessage("Mật khẩu phải có ít nhất 8 ký tự.");
      return;
    }
    if (password.length > 128) {
      setErrorMessage("Mật khẩu tối đa 128 ký tự.");
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsLoading(true);
    try {
      await authService.register({
        userName: username.trim(),
        email: email.trim(),
        password: password,
        phone: phone.trim() || undefined,
        dob: dob || undefined,
      });

      // Redirect to OTP verification with prefilled username
      router.push(`/verify-otp?username=${encodeURIComponent(username.trim())}`);
    } catch (err) {
      const msg = extractErrorMessage(err, "Đăng ký thất bại. Tên đăng nhập hoặc email có thể đã tồn tại.");
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
        <h1 className="text-lg sm:text-xl font-bold text-[#172B4D]">Tạo tài khoản mới</h1>
        <p className="mt-0.5 text-xs text-[#626F86]">
          Đăng ký để quản lý thông tin tài khoản cá nhân trong hệ thống
        </p>
      </div>

      {errorMessage && (
        <Alert type="error" className="mb-4" onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Tên đăng nhập"
            id="reg-username"
            type="text"
            placeholder="vd: nguyenvana"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={isLoading}
          />

          <Input
            label="Địa chỉ Email"
            id="reg-email"
            type="email"
            placeholder="vd: user@example.com"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <PasswordInput
            label="Mật khẩu"
            id="reg-password"
            placeholder="Tối thiểu 8 ký tự"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={isLoading}
          />

          <PasswordInput
            label="Xác nhận mật khẩu"
            id="reg-confirm-password"
            placeholder="Nhập lại mật khẩu"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Số điện thoại"
            id="reg-phone"
            type="tel"
            placeholder="vd: 0912345678"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            disabled={isLoading}
          />

          <Input
            label="Ngày sinh"
            id="reg-dob"
            type="date"
            value={dob}
            onChange={(e) => setDob(e.target.value)}
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
          Đăng ký tài khoản
        </Button>
      </form>

      <div className="mt-5 pt-4 border-t border-[#EBECF0] text-center text-xs text-[#626F86]">
        Đã có tài khoản?{" "}
        <Link href="/login" className="font-semibold text-[#0C66E4] hover:underline">
          Đăng nhập ngay
        </Link>
      </div>
    </div>
  );
}
