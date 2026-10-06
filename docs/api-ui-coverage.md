# BẢNG MA TRẬN API - UI COVERAGE DỰ ÁN UAM (USER ACCOUNT MANAGEMENT)

Tài liệu rà soát toàn diện contract API từ mã nguồn Spring Boot backend (`src/main/java`) và đối chiếu với kế hoạch triển khai Frontend Next.js App Router.

---

## 1. THÔNG SỐ CỐT LÕI HỆ THỐNG BACKEND

- **Base URL:** `http://localhost:8089` (Cấu hình qua `server.port: 8089`, context-path: `/`).
- **Swagger / OpenAPI:** `http://localhost:8089/swagger-ui.html` | Docs: `http://localhost:8089/v3/api-docs`.
- **Cơ chế xác thực (Authentication):**
  - Header: `Authorization: Bearer <accessToken>`.
  - Loại Token: JWT HMAC-SHA256 (`jwt.secret`).
  - Access Token Expiration: `360000 ms` (**6 phút** - rất ngắn, yêu cầu interceptor auto-refresh nghiêm ngặt).
  - Refresh Token Expiration: `1296000000 ms` (**15 ngày**).
  - Cơ chế Refresh Token: Lưu hash SHA-256 trong bảng `refresh_token`. Khi refresh, backend trả Access Token mới và giữ nguyên Refresh Token cũ.
  - Phân quyền (Authorization): Phân quyền kép tại `SecurityConfig` (URL matching) và `@PreAuthorize("hasRole('ADMIN')")` ở tầng Service/Controller.
  - Trạng thái người dùng (`UserStatus`):
    - `PENDING`: Đã đăng ký, cần xác thực OTP.
    - `ACTIVE`: Hoạt động bình thường.
    - `LOCKED`: Bị khóa (tự động khóa 15 phút nếu sai pass >= 5 lần, hoặc khóa vĩnh viễn do Admin).
    - `DISABLED`: Vô hiệu hóa / Xóa mềm (chỉ Admin mới kích hoạt lại được).
    - **Lưu ý Security:** Nếu user ở trạng thái `PENDING`, `LOCKED` hoặc `DISABLED`, `JwtAuthenticationFilter` sẽ từ chối với `401 Unauthorized` ngay cả khi Access Token còn hạn!

---

## 2. BẢNG MA TRẬN API - UI COVERAGE & UNIT TEST

| HTTP Method | Endpoint | Quyền (Role/Public) | Request / Response DTO | Màn hình / Action trên FE | Trạng thái Triển khai & Unit Test |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/register` | Public | **Req:** `RegisterRequest`<br>**Res:** `String` | Màn hình Đăng ký (`/register`): Form nhập username, email, mật khẩu | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **POST** | `/loginWithJwt` | Public | **Req:** `LoginRequest`<br>**Res:** `LoginResponse` | Màn hình Đăng nhập (`/login`): Form đăng nhập | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **POST** | `/api/v1/auth/verify` | Public | **Req:** `VertifyRequest`<br>**Res:** `String` | Màn hình OTP (`/verify-otp`): Bẫy text HTTP 200 thành `ApiError` | **HOÀN THÀNH** (Covered in `api-client.test.mjs`) |
| **POST** | `/refreshToken` | Public | **Req:** `RefreshTokenRequest`<br>**Res:** `LoginResponse` | `api-client.ts`: Tự động refresh khi 401 & chống race-condition | **HOÀN THÀNH** (Covered in `api-client.test.mjs`) |
| **POST** | `/logout` | Public / Bearer | **Req:** `RefreshTokenRequest`<br>**Res:** `204 No Content` | Header: Nút "Đăng xuất" kèm Modal xác nhận | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PUT** | `/changePassword` | Authenticated | **Req:** `ChangePasswordRequest`<br>**Res:** `String` | Màn hình Bảo mật (`/security`): Form đổi mật khẩu | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **GET** | `/me` | Authenticated | **Req:** None<br>**Res:** `UserResponse` | Header, Profile (`/profile`), Dashboard (`/dashboard`) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PUT** | `/me/update` | Authenticated | **Req:** `UpdateUserRequest`<br>**Res:** `String` | Màn hình Hồ sơ cá nhân (`/profile`): Form cập nhật | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PUT** | `/me/email` | Authenticated | **Req:** `ChangeEmailRequest`<br>**Res:** `String` | Màn hình Hồ sơ cá nhân (`/profile`): Đổi email bước 1 | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **POST** | `/me/email/verify` | Authenticated | **Req:** `VerifyChangeEmailRequest`<br>**Res:** `String` | Màn hình Hồ sơ cá nhân (`/profile`): Xác thực OTP đổi email | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **GET** | `/api/admin/users` | ROLE_ADMIN | **Req:** Query params<br>**Res:** `List<AdminUserResponse>` | Quản trị User (`/admin/users`): Bảng danh sách, bộ lọc, phân trang | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **GET** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Chi tiết User (`/admin/users/[id]`): Xem hồ sơ chi tiết | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PUT** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** `AdminUpdateUserRequest`<br>**Res:** `AdminUserResponse` | Chi tiết User (`/admin/users/[id]`): Cập nhật thông tin | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **DELETE** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `204 No Content` | Bảng Admin: Nút "Vô hiệu hóa / Xóa" (Modal confirm) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PATCH** | `/api/admin/users/{id}/lock` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Quản trị User: Nút "Khóa tài khoản" (Modal confirm) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PATCH** | `/api/admin/users/{id}/unlock` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Quản trị User: Nút "Mở khóa tài khoản" (Modal confirm) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PATCH** | `/api/admin/users/{id}/disable` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Quản trị User: Nút "Vô hiệu hóa" (Modal confirm) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **PATCH** | `/api/admin/users/{id}/activate` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Quản trị User: Nút "Kích hoạt" (Modal confirm) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **GET** | `/api/roles` | ROLE_ADMIN | **Req:** None<br>**Res:** `List<Role>` | Chi tiết User (`/admin/users/[id]`): Dropdown gán vai trò | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **POST** | `/api/roles/assign` | ROLE_ADMIN | **Req:** `AssignRoleRequest`<br>**Res:** `String` | Chi tiết User (`/admin/users/[id]`): Gán vai trò mới | **HOÀN THÀNH** (Covered in `services.test.mjs`) |
| **DELETE** | `/api/roles/remove` | ROLE_ADMIN | **Req:** `AssignRoleRequest`<br>**Res:** `String` | Chi tiết User (`/admin/users/[id]`): Gỡ vai trò (chặn ROLE_USER) | **HOÀN THÀNH** (Covered in `services.test.mjs`) |

---

## 3. TỔNG KẾT KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (UNIT TESTS)

- Lệnh chạy kiểm thử: `npm test` (sử dụng Node.js native test runner `node --test tests/*.test.mjs`).
- Số lượng test suites: **4 file test**.
- Tổng số test cases: **31 / 31 test cases PASS (100%)**.
  - `tests/http.test.mjs`: **10 tests** (Reverse proxy URL routing, JSON serialization, text/empty parsing, error wrapping, path escape security).
  - `tests/token-storage.test.mjs`: **6 tests** (Token storage, JWT payload decode, expiration check, `isAdmin()`, `clearTokens()`).
  - `tests/api-client.test.mjs`: **6 tests** (Bearer token injection, skipAuth, 403 Forbidden no-refresh, 401 refresh queuing, `/verify` OTP HTTP 200 text error translation, Vietnamese error messages).
  - `tests/services.test.mjs`: **9 tests** (`authService`, `userService`, `adminService` actions).
