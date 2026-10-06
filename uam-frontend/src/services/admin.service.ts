import { apiClient } from "../lib/api-client.ts";
import type {
  AdminUserResponse,
  AdminUpdateUserRequest,
  AdminSearchFilter,
  Role,
  AssignRoleRequest,
} from "../types/index.ts";

export const adminService = {
  /**
   * Tra cứu danh sách người dùng với các bộ lọc đa tiêu chí (GET /api/admin/users)
   */
  async searchUsers(filter: AdminSearchFilter = {}): Promise<AdminUserResponse[]> {
    const params = new URLSearchParams();
    if (filter.username?.trim()) params.set("username", filter.username.trim());
    if (filter.email?.trim()) params.set("email", filter.email.trim());
    if (filter.role?.trim()) params.set("role", filter.role.trim());
    if (filter.status?.trim()) params.set("status", filter.status.trim());

    const queryString = params.toString() ? `?${params.toString()}` : "";
    return await apiClient<AdminUserResponse[]>(`/api/admin/users${queryString}`, {
      method: "GET",
    });
  },

  /**
   * Xem chi tiết người dùng theo ID (GET /api/admin/users/{id})
   */
  async getUserById(id: number): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}`, {
      method: "GET",
    });
  },

  /**
   * Cập nhật thông tin người dùng từ phía Admin (PUT /api/admin/users/{id})
   */
  async updateUser(id: number, data: AdminUpdateUserRequest): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}`, {
      method: "PUT",
      body: {
        username: data.username,
        email: data.email,
        fullname: data.fullname,
        phone: data.phone,
        doB: data.doB,
      },
    });
  },

  /**
   * Khóa tài khoản người dùng (PATCH /api/admin/users/{id}/lock)
   */
  async lockUser(id: number): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}/lock`, {
      method: "PATCH",
    });
  },

  /**
   * Mở khóa tài khoản người dùng (PATCH /api/admin/users/{id}/unlock)
   */
  async unlockUser(id: number): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}/unlock`, {
      method: "PATCH",
    });
  },

  /**
   * Vô hiệu hóa tài khoản người dùng (PATCH /api/admin/users/{id}/disable)
   */
  async disableUser(id: number): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}/disable`, {
      method: "PATCH",
    });
  },

  /**
   * Kích hoạt lại tài khoản người dùng (PATCH /api/admin/users/{id}/activate)
   */
  async activateUser(id: number): Promise<AdminUserResponse> {
    return await apiClient<AdminUserResponse>(`/api/admin/users/${id}/activate`, {
      method: "PATCH",
    });
  },

  /**
   * Xóa tài khoản người dùng (DELETE /api/admin/users/{id})
   */
  async deleteUser(id: number): Promise<void> {
    return await apiClient<void>(`/api/admin/users/${id}`, {
      method: "DELETE",
    });
  },

  /**
   * Lấy danh mục tất cả vai trò trong hệ thống (GET /api/roles)
   */
  async getRoles(): Promise<Role[]> {
    return await apiClient<Role[]>("/api/roles", {
      method: "GET",
    });
  },

  /**
   * Gán vai trò cho người dùng (POST /api/roles/assign)
   * Gửi cả username và userName để tương thích với Spring Boot DTO
   */
  async assignRole(data: AssignRoleRequest): Promise<string> {
    const uname = (data.username || data.userName || "").trim();
    return await apiClient<string>("/api/roles/assign", {
      method: "POST",
      body: {
        username: uname,
        userName: uname,
        roleName: data.roleName.trim(),
      },
    });
  },

  /**
   * Gỡ bỏ vai trò khỏi người dùng (DELETE /api/roles/remove)
   * Gửi cả username và userName để tương thích với Spring Boot DTO
   */
  async removeRole(data: AssignRoleRequest): Promise<string> {
    const uname = (data.username || data.userName || "").trim();
    return await apiClient<string>("/api/roles/remove", {
      method: "DELETE",
      body: {
        username: uname,
        userName: uname,
        roleName: data.roleName.trim(),
      },
    });
  },
};
