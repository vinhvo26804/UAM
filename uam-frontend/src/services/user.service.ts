import { apiClient } from "../lib/api-client.ts";
import type {
  UserResponse,
  UpdateUserRequest,
  ChangeEmailRequest,
  VerifyChangeEmailRequest,
} from "../types/index.ts";

export const userService = {
  /**
   * Lấy thông tin cá nhân của người dùng hiện tại (GET /me)
   */
  async getProfile(): Promise<UserResponse> {
    return await apiClient<UserResponse>("/me", {
      method: "GET",
    });
  },

  /**
   * Cập nhật thông tin cá nhân (PUT /me/update)
   */
  async updateProfile(data: UpdateUserRequest): Promise<UserResponse> {
    const rawDob = data.dob || data.doB;
    return await apiClient<UserResponse>("/me/update", {
      method: "PUT",
      body: {
        fullname: data.fullname?.trim() || "",
        phone: data.phone?.trim() || "",
        dob: rawDob && rawDob.trim() !== "" ? rawDob.trim() : null,
      },
    });
  },

  /**
   * Gửi yêu cầu đổi email (PUT /me/email)
   */
  async requestChangeEmail(data: ChangeEmailRequest): Promise<string> {
    const targetEmail = (data.newEmail || data.email || "").trim();
    return await apiClient<string>("/me/email", {
      method: "PUT",
      body: {
        email: targetEmail,
        newEmail: targetEmail,
      },
    });
  },

  /**
   * Xác thực mã OTP đổi email (POST /me/email/verify)
   * Gửi cả otpCode và otp để tương thích backend
   */
  async verifyChangeEmail(data: VerifyChangeEmailRequest): Promise<string> {
    const code = (data.otpCode || data.otp || "").trim();
    return await apiClient<string>("/me/email/verify", {
      method: "POST",
      body: {
        otpCode: code,
        otp: code,
      },
    });
  },
};
