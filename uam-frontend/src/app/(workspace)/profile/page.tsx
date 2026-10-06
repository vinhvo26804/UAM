"use client";

import { useState, useEffect, FormEvent } from "react";
import { userService } from "@/services/user.service";
import { UserResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert } from "@/components/ui/alert";
import { StatusBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { extractErrorMessage } from "@/lib/api-client";

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Profile form state
  const [fullname, setFullname] = useState("");
  const [phone, setPhone] = useState("");
  const [dob, setDob] = useState("");
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Email form state
  const [newEmail, setNewEmail] = useState("");
  const [isRequestingEmailOtp, setIsRequestingEmailOtp] = useState(false);
  const [emailOtpSent, setEmailOtpSent] = useState(false);
  const [emailOtpCode, setEmailOtpCode] = useState("");
  const [isVerifyingEmail, setIsVerifyingEmail] = useState(false);
  const [emailSuccess, setEmailSuccess] = useState<string | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchProfile() {
      try {
        const data = await userService.getProfile();
        setProfile(data);
        setFullname(data.fullname || "");
        setPhone(data.phone || "");
        setDob(data.dob || "");
      } catch (err) {
        setProfileError(extractErrorMessage(err, "Không thể tải thông tin hồ sơ."));
      } finally {
        setIsLoading(false);
      }
    }

    fetchProfile();
  }, []);

  const handleUpdateProfile = async (e: FormEvent) => {
    e.preventDefault();
    setProfileSuccess(null);
    setProfileError(null);
    setIsUpdatingProfile(true);

    try {
      await userService.updateProfile({
        fullname: fullname.trim() || undefined,
        phone: phone.trim() || undefined,
        doB: dob || undefined,
      });

      setProfileSuccess("Cập nhật thông tin hồ sơ thành công!");
      if (profile) {
        setProfile({
          ...profile,
          fullname: fullname.trim(),
          phone: phone.trim(),
          dob: dob,
        });
      }
    } catch (err) {
      setProfileError(extractErrorMessage(err, "Cập nhật hồ sơ thất bại."));
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleRequestEmailOtp = async (e: FormEvent) => {
    e.preventDefault();
    setEmailSuccess(null);
    setEmailError(null);

    if (!newEmail.trim() || !newEmail.includes("@")) {
      setEmailError("Vui lòng nhập địa chỉ email hợp lệ.");
      return;
    }

    setIsRequestingEmailOtp(true);
    try {
      await userService.requestChangeEmail({ newEmail: newEmail.trim() });
      setEmailOtpSent(true);
      setEmailSuccess(`Mã OTP đã được gửi đến email ${newEmail.trim()}. Vui lòng kiểm tra hộp thư!`);
    } catch (err) {
      setEmailError(extractErrorMessage(err, "Không thể gửi OTP đổi email."));
    } finally {
      setIsRequestingEmailOtp(false);
    }
  };

  const handleVerifyEmailOtp = async (e: FormEvent) => {
    e.preventDefault();
    setEmailSuccess(null);
    setEmailError(null);

    if (!emailOtpCode.trim()) {
      setEmailError("Vui lòng nhập mã OTP xác thực.");
      return;
    }

    setIsVerifyingEmail(true);
    try {
      await userService.verifyChangeEmail({
        newEmail: newEmail.trim(),
        otp: emailOtpCode.trim(),
      });
      setEmailSuccess("Đổi địa chỉ email thành công!");
      setEmailOtpSent(false);
      setEmailOtpCode("");
      setNewEmail("");
    } catch (err) {
      setEmailError(extractErrorMessage(err, "Mã OTP xác thực không đúng hoặc đã hết hạn."));
    } finally {
      setIsVerifyingEmail(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-44 w-full" />
        <Skeleton className="h-36 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Header Profile Identity */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#DCDFE4] pb-3">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] leading-tight">
            Hồ sơ cá nhân
          </h1>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Cập nhật họ tên, số điện thoại, ngày sinh và địa chỉ email định danh
          </p>
        </div>

        {profile && (
          <div className="flex items-center gap-2.5">
            <div className="grid h-8 w-8 place-items-center rounded-full bg-[#E9F2FF] text-[#0C66E4] text-xs font-bold border border-[#85B8FF]">
              {profile.username ? profile.username.slice(0, 2).toUpperCase() : "U"}
            </div>
            <div>
              <div className="text-xs font-semibold text-[#172B4D]">{profile.username}</div>
              {profile.status && <StatusBadge status={profile.status} />}
            </div>
          </div>
        )}
      </div>

      {/* Basic Profile Information Card */}
      <div className="rounded border border-[#DCDFE4] bg-white p-4 sm:p-5">
        <div className="border-b border-[#EBECF0] pb-2.5 mb-4">
          <h2 className="text-sm font-bold text-[#172B4D]">Thông tin cá nhân</h2>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Các thông tin cơ bản dùng để hiển thị và xác minh danh tính
          </p>
        </div>

        {profileSuccess && (
          <Alert type="success" className="mb-4" onClose={() => setProfileSuccess(null)}>
            {profileSuccess}
          </Alert>
        )}

        {profileError && (
          <Alert type="error" className="mb-4" onClose={() => setProfileError(null)}>
            {profileError}
          </Alert>
        )}

        <form onSubmit={handleUpdateProfile} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <Input
              label="Tên đăng nhập"
              id="prof-username"
              type="text"
              value={profile?.username || ""}
              disabled
              helperText="Tên đăng nhập là định danh cố định không thể thay đổi"
              className="bg-[#F1F2F4] text-[#8590A2] cursor-not-allowed"
            />

            <Input
              label="Họ và tên"
              id="prof-fullname"
              type="text"
              placeholder="Nhập họ và tên đầy đủ"
              value={fullname}
              onChange={(e) => setFullname(e.target.value)}
              disabled={isUpdatingProfile}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <Input
              label="Số điện thoại"
              id="prof-phone"
              type="tel"
              placeholder="Nhập số điện thoại"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              disabled={isUpdatingProfile}
            />

            <Input
              label="Ngày sinh"
              id="prof-dob"
              type="date"
              value={dob}
              onChange={(e) => setDob(e.target.value)}
              disabled={isUpdatingProfile}
            />
          </div>

          <div className="pt-2 flex justify-end">
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isUpdatingProfile}
            >
              Lưu thay đổi
            </Button>
          </div>
        </form>
      </div>

      {/* Change Email Card */}
      <div className="rounded border border-[#DCDFE4] bg-white p-4 sm:p-5">
        <div className="border-b border-[#EBECF0] pb-2.5 mb-4">
          <h2 className="text-sm font-bold text-[#172B4D]">Thay đổi địa chỉ Email</h2>
          <p className="mt-0.5 text-xs text-[#626F86]">
            Hệ thống sẽ gửi mã OTP xác nhận đến hòm thư mới để hoàn tất việc đổi email
          </p>
        </div>

        {emailSuccess && (
          <Alert type="success" className="mb-4" onClose={() => setEmailSuccess(null)}>
            {emailSuccess}
          </Alert>
        )}

        {emailError && (
          <Alert type="error" className="mb-4" onClose={() => setEmailError(null)}>
            {emailError}
          </Alert>
        )}

        {!emailOtpSent ? (
          <form onSubmit={handleRequestEmailOtp} className="space-y-3.5 max-w-md">
            <Input
              label="Địa chỉ Email mới"
              id="new-email"
              type="email"
              placeholder="vd: tennguoidung@example.com"
              required
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              disabled={isRequestingEmailOtp}
            />

            <Button
              type="submit"
              variant="outline"
              size="sm"
              isLoading={isRequestingEmailOtp}
            >
              Gửi mã xác nhận OTP
            </Button>
          </form>
        ) : (
          <form onSubmit={handleVerifyEmailOtp} className="space-y-3.5 max-w-md">
            <div className="p-2.5 rounded bg-[#E9F2FF] border border-[#85B8FF] text-xs text-[#09326C]">
              Email mới đang chờ xác thực: <strong>{newEmail}</strong>
            </div>

            <Input
              label="Nhập mã OTP đã nhận"
              id="email-otp"
              type="text"
              maxLength={6}
              placeholder="Nhập 6 chữ số OTP"
              required
              value={emailOtpCode}
              onChange={(e) => setEmailOtpCode(e.target.value.replace(/\D/g, ""))}
              disabled={isVerifyingEmail}
            />

            <div className="flex items-center gap-2">
              <Button
                type="submit"
                variant="primary"
                size="sm"
                isLoading={isVerifyingEmail}
              >
                Xác thực & Hoàn tất
              </Button>

              <Button
                type="button"
                variant="secondary"
                size="sm"
                disabled={isVerifyingEmail}
                onClick={() => setEmailOtpSent(false)}
              >
                Nhập lại email
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
