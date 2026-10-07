export interface Role {
  id?: number;
  name?: string;
  roleName?: string;
  description?: string;
}

export interface AssignRoleRequest {
  userName?: string;
  username?: string;
  roleName: string;
}

export type UserStatus = "ACTIVE" | "LOCKED" | "DISABLED" | "PENDING";

export interface UserResponse {
  id: number;
  username: string;
  phone?: string;
  dob?: string;
  doB?: string;
  status: UserStatus;
  fullname?: string;
  roles: Array<string | Role>;
  email?: string;
}

export interface AdminUserResponse {
  id: number;
  username: string;
  email: string;
  fullname?: string;
  doB?: string;
  dob?: string;
  phone?: string;
  status: UserStatus;
  roles: Array<string | Role>;
  lockUntill?: string;
  lockedUntil?: string;
  failedLoginAttemps: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginRequest {
  userName: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
}

export interface RegisterRequest {
  userName: string;
  email: string;
  password: string;
  phone?: string;
  dob?: string;
}

export interface VertifyRequest {
  userName: string;
  otpCode: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword?: string;
  oldPassword?: string;
  newPassword: string;
  confirmNewPassword?: string;
  confirmPassword?: string;
}

export interface UpdateUserRequest {
  fullname?: string;
  phone?: string;
  dob?: string;
  doB?: string;
}

export interface ChangeEmailRequest {
  email?: string;
  newEmail: string;
}

export interface VerifyChangeEmailRequest {
  otp?: string;
  otpCode?: string;
  newEmail?: string;
}

export interface AdminUpdateUserRequest {
  username?: string;
  email?: string;
  fullname?: string;
  phone?: string;
  doB?: string;
  dob?: string;
}

export interface AdminSearchFilter {
  username?: string;
  email?: string;
  role?: string;
  status?: string;
}

export interface JwtClaims {
  sub?: string;
  roles?: string[] | string;
  role?: string;
  type?: string;
  exp?: number;
  iat?: number;
  jti?: string;
}
