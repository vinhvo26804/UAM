"use client";

import { useState, FormEvent } from "react";
import { authService } from "@/services/auth.service";
import { Button } from "@/components/ui/button";
import { PasswordInput } from "@/components/ui/password-input";
import { Alert } from "@/components/ui/alert";
import { extractErrorMessage } from "@/lib/api-client";

export default function SecurityPage() {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSuccessMessage(null);
    setErrorMessage(null);

    if (!oldPassword) {
      setErrorMessage("Vui lòng nhập mật khẩu hiện tại.");
      return;
    }
    if (newPassword.length < 8) {
      setErrorMessage("Mật khẩu mới phải có tối thiểu 8 ký tự.");
      return;
    }
    if (newPassword.length > 128) {
      setErrorMessage("Mật khẩu mới tối đa 128 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp.");
      return;
    }
    if (oldPassword === newPassword) {
      setErrorMessage("Mật khẩu mới không được trùng với mật khẩu cũ.");
      return;
    }

    setIsLoading(true);
    try {
      await authService.changePassword({
        oldPassword,
        newPassword,
        confirmPassword,
      });

      setSuccessMessage("Đổi mật khẩu thành công! Mật khẩu mới đã có hiệu lực.");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu cũ."));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Page Title */}
      <div className="border-b border-[#DCDFE4] pb-3">
        <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] leading-tight">
          Bảo mật tài khoản
        </h1>
        <p className="mt-0.5 text-xs text-[#626F86]">
          Quản lý mật khẩu đăng nhập để bảo vệ an toàn cho tài khoản cá nhân
        </p>
      </div>

      {/* Change Password Card */}
      <div className="rounded border border-[#DCDFE4] bg-white p-4 sm:p-5">
        <div className="border-b border-[#EBECF0] pb-2.5 mb-4">
          <h2 className="text-sm font-bold text-[#172B4D]">Đổi mật khẩu</h2>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Mật khẩu mới yêu cầu tối thiểu 6 ký tự
          </p>
        </div>

        {successMessage && (
          <Alert type="success" className="mb-4" onClose={() => setSuccessMessage(null)}>
            {successMessage}
          </Alert>
        )}

        {errorMessage && (
          <Alert type="error" className="mb-4" onClose={() => setErrorMessage(null)}>
            {errorMessage}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="space-y-3.5 max-w-md">
          <PasswordInput
            label="Mật khẩu hiện tại"
            id="sec-old-password"
            placeholder="Nhập mật khẩu hiện tại"
            required
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            disabled={isLoading}
          />

          <PasswordInput
            label="Mật khẩu mới"
            id="sec-new-password"
            placeholder="Tối thiểu 8 ký tự"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={isLoading}
          />

          <PasswordInput
            label="Xác nhận mật khẩu mới"
            id="sec-confirm-password"
            placeholder="Nhập lại mật khẩu mới"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={isLoading}
          />

          <div className="pt-1">
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isLoading}
            >
              Cập nhật mật khẩu
            </Button>
          </div>
        </form>
      </div>

      {/* Security Best Practices */}
      <div className="rounded border border-[#DCDFE4] bg-[#FAFBFC] p-4">
        <h3 className="text-xs font-bold text-[#172B4D] mb-1.5 uppercase tracking-wider">
          Nguyên tắc an toàn tài khoản
        </h3>
        <ul className="text-xs text-[#626F86] space-y-1 list-disc list-inside leading-relaxed">
          <li>Sử dụng mật khẩu có độ dài từ 8 ký tự trở lên, kết hợp chữ số và ký tự đặc biệt.</li>
          <li>Không sử dụng lại mật khẩu đã dùng cho các dịch vụ khác.</li>
          <li>Đổi mật khẩu định kỳ 3-6 tháng một lần để phòng ngừa rủi ro.</li>
          <li>Hệ thống tự động khóa tài khoản tạm thời nếu nhập sai mật khẩu quá 5 lần liên tiếp.</li>
        </ul>
      </div>
    </div>
  );
}
