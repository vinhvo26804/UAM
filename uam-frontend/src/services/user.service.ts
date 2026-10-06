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
    return await apiClient<UserResponse>("/me/update", {
      method: "PUT",
      body: {
        fullname: data.fullname?.trim() || "",
        phone: data.phone?.trim() || "",
        dob: data.dob || data.doB || "",
      },
    });
  },

  /**
   * Gửi yêu cầu đổi email (PUT /me/email)
   */
  async requestChangeEmail(data: ChangeEmailRequest): Promise<string> {
    return await apiClient<string>("/me/email", {
      method: "PUT",
      body: {
        newEmail: data.newEmail.trim(),
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
