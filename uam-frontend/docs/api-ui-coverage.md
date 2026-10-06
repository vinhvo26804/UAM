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

## 2. BẢNG MA TRẬN API - UI COVERAGE

| HTTP Method | Endpoint | Quyền (Role/Public) | Request / Response DTO | Màn hình / Action trên FE | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/register` | Public | **Req:** `RegisterRequest` (`userName`, `password`, `email`)<br>**Res:** `String` ("Register Success") | Màn hình Đăng ký (`/register`): Form nhập username, email, mật khẩu | **Cần UI** |
| **POST** | `/loginWithJwt` | Public | **Req:** `LoginRequest` (`userName`, `password`)<br>**Res:** `LoginResponse` (`accessToken`, `refreshToken`, `tokenType`) | Màn hình Đăng nhập (`/login`): Form đăng nhập | **Cần UI** |
| **POST** | `/api/v1/auth/verify` | Public | **Req:** `VertifyRequest` (`userName`, `otpCode`)<br>**Res:** `String` ("Verify Success" / thông báo lỗi) | Màn hình Xác thực OTP (`/verify-otp`): Form nhập OTP sau đăng ký | **Cần UI** (Cần bẫy lỗi text HTTP 200) |
| **POST** | `/refreshToken` | Public | **Req:** `RefreshTokenRequest` (`refreshToken`)<br>**Res:** `LoginResponse` (`accessToken`, `refreshToken`, `tokenType`) | `api-client.ts`: Interceptor tự động refresh khi access token hết hạn | **Gọi tự động** (Không cần UI riêng) |
| **POST** | `/logout` | Public (Nhận Bearer / Token) | **Req:** `RefreshTokenRequest` (`refreshToken`)<br>**Res:** `204 No Content` | Header / Menu: Nút "Đăng xuất" | **Cần UI** |
| **PUT** | `/changePassword` | Authenticated | **Req:** `ChangePasswordRequest` (`currentPassword`, `newPassword`, `confirmNewPassword`)<br>**Res:** `String` ("Password was be changed") | Màn hình Bảo mật cá nhân (`/security`): Form đổi mật khẩu | **Cần UI** |
| **GET** | `/test` | Authenticated | **Req:** None<br>**Res:** `String` (" Access success") | Trang kiểm tra kết nối / Health check nội bộ | **Gọi tự động** / Debug |
| **GET** | `/me` | Authenticated | **Req:** None<br>**Res:** `UserResponse` (`id`, `username`, `phone`, `dob`, `status`, `fullname`, `roles`) | Header, Profile cá nhân (`/profile`), Dashboard (`/dashboard`) | **Cần UI** |
| **PUT** | `/me/update` *(Lưu ý path)* | Authenticated | **Req:** `UpdateUserRequest` (`fullname`, `doB`, `phone`)<br>**Res:** `String` ("Update user successfully") | Màn hình Hồ sơ cá nhân (`/profile`): Form cập nhật họ tên, ngày sinh, SĐT | **Cần UI** |
| **PUT** | `/me/email` | Authenticated | **Req:** `ChangeEmailRequest` (`email`)<br>**Res:** `String` ("OTP sent to new email") | Màn hình Hồ sơ cá nhân (`/profile`): Modal/Form yêu cầu đổi email | **Cần UI** |
| **POST** | `/me/email/verify` | Authenticated | **Req:** `VerifyChangeEmailRequest` (`otpCode`)<br>**Res:** `String` ("Email changed successfully") | Màn hình Hồ sơ cá nhân (`/profile`): Dialog nhập OTP xác thực email mới | **Cần UI** |
| **GET** | `/search` | ROLE_ADMIN | **Req:** Query params (`username`, `email`, `role`, `status`)<br>**Res:** `List<UserResponse>` | Dự phòng (Trùng chức năng với `/api/admin/users`, chỉ trả `UserResponse` thiếu email) | **Backend chưa tối ưu** (FE ưu tiên dùng `/api/admin/users`) |
| **GET** | `/api/admin/users` | ROLE_ADMIN | **Req:** Query params (`username`, `email`, `role`, `status`)<br>**Res:** `List<AdminUserResponse>` (`id`, `username`, `email`, `fullname`, `doB`, `phone`, `status`, `roles`, `lockUntill`) | Màn hình Quản trị User (`/admin/users`): Bảng danh sách tài khoản, bộ lọc tìm kiếm | **Cần UI** |
| **GET** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Màn hình Chi tiết User (`/admin/users/[id]`): Xem hồ sơ chi tiết | **Cần UI** |
| **PUT** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** `AdminUpdateUserRequest` (`email`, `fullname`, `doB`, `phone`, `username?`)<br>**Res:** `AdminUserResponse` | Màn hình Chi tiết User (`/admin/users/[id]`): Form sửa thông tin user | **Cần UI** |
| **DELETE** | `/api/admin/users/{id}` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `204 No Content` (Soft delete -> DISABLED) | Bảng Admin User: Action "Vô hiệu hóa / Xóa người dùng" (Modal confirm) | **Cần UI** |
| **PATCH** | `/api/admin/users/{id}/lock` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Màn hình Quản trị User: Nút "Khóa tài khoản" (Modal confirm) | **Cần UI** |
| **PATCH** | `/api/admin/users/{id}/unlock` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Màn hình Quản trị User: Nút "Mở khóa tài khoản" (Modal confirm) | **Cần UI** |
| **PATCH** | `/api/admin/users/{id}/disable` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Màn hình Quản trị User: Nút "Vô hiệu hóa" (Modal confirm) | **Cần UI** |
| **PATCH** | `/api/admin/users/{id}/activate` | ROLE_ADMIN | **Req:** Path variable `id`<br>**Res:** `AdminUserResponse` | Màn hình Quản trị User: Nút "Kích hoạt tài khoản" (Modal confirm) | **Cần UI** |
| **GET** | `/api/roles` | ROLE_ADMIN | **Req:** None<br>**Res:** `List<Role>` (`id`, `name`, `permissions`) | Màn hình Quản trị Role/User: Dropdown chọn Role khi gán quyền | **Cần UI** |
| **GET** | `/api/roles/{roleName}` | ROLE_ADMIN | **Req:** Path variable `roleName`<br>**Res:** `Role` (`id`, `name`, `permissions`) | Xem chi tiết quyền hạn của Role | **Cần UI** |
| **POST** | `/api/roles/assign` | ROLE_ADMIN | **Req:** `AssignRoleRequest` (`username`, `roleName`)<br>**Res:** `String` ("Assign role successfully") | Màn hình Chi tiết User: Action "Gán Role cho User" | **Cần UI** |
| **DELETE** | `/api/roles/remove` | ROLE_ADMIN | **Req:** `AssignRoleRequest` (`username`, `roleName`)<br>**Res:** `String` ("Remove role successfully") | Màn hình Chi tiết User: Action "Gỡ Role khỏi User" (Chặn gỡ ROLE_USER) | **Cần UI** |
| **POST** | `/forgot-password` (hoặc tương đương) | Public | **Req:** `ForgotPasswordRequest` (`userName`)<br>**Res:** Chưa có Controller | Màn hình Quên mật khẩu (`/forgot-password`) | **Backend chưa hỗ trợ** (Xem mục 3) |
| **POST** | `/reset-password` | Public | **Req:** Chưa có DTO / Service / Controller | Màn hình Đặt lại mật khẩu | **Backend chưa hỗ trợ** (Xem mục 3) |
| **POST** | `/resend-otp` | Public | **Req:** Chưa có DTO / Service / Controller | Nút "Gửi lại OTP" ở màn verify | **Backend chưa hỗ trợ** (Xem mục 3) |

---

## 3. TỔNG HỢP CÁC ĐIỂM BẤT THƯỜNG & THIẾU SÓT CỦA BACKEND

Qua quá trình rà soát chi tiết từng dòng code trong Controller, Service, DTO, SecurityConfig và Migration, phát hiện các vấn đề kỹ thuật quan trọng sau:

### 1. Thiếu sót về Endpoint / Nghiệp vụ chưa hoàn thiện:
1. **Chưa có API Quên mật khẩu & Đặt lại mật khẩu (Forgot / Reset Password):**
   - Mặc dù backend có class `ForgotPasswordRequest` và method `AuthService.forgotPassword()`, nhưng **không có bất kỳ Controller nào phơi endpoint ra ngoài**.
   - Chưa hề có nghiệp vụ và API `resetPassword(otp, newPassword)` để người dùng hoàn tất đặt lại mật khẩu sau khi nhận OTP.
   - Các file test liên quan đến Password Reset trong backend hiện đang bị lỗi biên dịch vì thiếu class (`PasswordResetService`, `PasswordResetController`...).
   - **Tác động FE:** Màn hình `/forgot-password` và `/reset-password` chỉ có thể dựng UI Form demo hoặc hiển thị thông báo "Tính năng đang được backend hoàn thiện", không thể kết nối API thực tế.
2. **Không có API Gửi lại mã OTP (Resend OTP):**
   - OTP đăng ký tài khoản hết hạn sau 5 phút. Nếu người dùng không nhập kịp, hiện tại backend không có API để xin cấp lại mã OTP mới.
3. **Endpoint `/api/v1/auth/login` bị comment:**
   - Trong `AuthController.java`, endpoint `/api/v1/auth/login` đã bị comment out. Endpoint đăng nhập thực tế duy nhất là `/loginWithJwt`.
4. **Không có API Quản lý Session / Thiết bị (Device Management):**
   - Backend chỉ lưu `RefreshToken` trong database và không lưu thông tin User-Agent, IP, Device Name. Do đó, FE không thể hiển thị danh sách các thiết bị đang đăng nhập để người dùng thu hồi từng thiết bị riêng lẻ.

### 2. Sự không đồng nhất trong Data Contract (Naming Convention & Response):
1. **Lệch tên trường Tên đăng nhập (`userName` vs `username`):**
   - Nhóm DTO Request Auth: `LoginRequest`, `RegisterRequest`, `VertifyRequest` sử dụng **`userName`** (chữ `N` viết hoa).
   - Nhóm Response & DTO Admin: `UserResponse`, `AdminUserResponse`, `AdminUpdateUserRequest`, `AssignRoleRequest` sử dụng **`username`** (chữ `n` viết thường).
2. **Lệch tên trường Ngày sinh (`dob` vs `doB`):**
   - `UserResponse` (trả về từ `/me`) dùng **`dob`** (chữ `o`, `b` thường).
   - `AdminUserResponse`, `UpdateUserRequest`, `AdminUpdateUserRequest` dùng **`doB`** (chữ `B` viết hoa).
3. **Lệch chính tả thời gian khóa (`lockUntill`):**
   - `AdminUserResponse` dùng trường **`lockUntill`** (chữ `l` lặp 2 lần).
4. **Endpoint `/me` (`UserResponse`) KHÔNG có trường `email`:**
   - DTO `UserResponse` chỉ gồm: `id`, `username`, `phone`, `dob`, `status`, `fullname`, `roles`. Không có trường `email`. Muốn biết email của chính mình, user thông thường không có API để đọc (chỉ có Admin đọc được qua `AdminUserResponse`).
5. **Path endpoint `/me/update` bị thiếu dấu gạch chéo đầu:**
   - Trong `UserController.java`, method viết `@PutMapping("me/update")` (thiếu gạch chéo đầu `/`). Spring Boot vẫn map thành `/me/update`.
6. **Xác thực OTP trả HTTP 200 khi thất bại:**
   - Endpoint `POST /api/v1/auth/verify`: Nếu OTP sai, hết hạn hoặc không tìm thấy, code trả về HTTP 200 kèm body text: `"Verify failed"`, `"OTP expired!"`, `"OTP not found"`.
   - **Tác động FE:** `api-client` không được chỉ dựa vào `res.ok` (status 200) mà bắt buộc phải kiểm tra nội dung text trả về có đúng bằng `"Verify Success"` hay không.

### 3. Vấn đề gửi thông báo (Email / SMS Notification):
1. **Đăng ký không gửi Email OTP:** `AuthService.register()` sinh chuỗi OTP 6 số và lưu vào bảng `verify`, nhưng không có logic gọi mail sender.
2. **Đổi Email chỉ in OTP ra console log server:** `UserService.changeEmail()` in mã OTP qua `System.out.println("CHANGE EMAIL OTP: " + otpCode);` chứ không gửi mail tới hòm thư người dùng.

### 4. Thiếu sót về Cấu trúc Lỗi & Phân trang (Pagination):
1. **Không có Global Exception Handler chuẩn hóa:**
   - Controller chỉ bắt `ResponseStatusException` cục bộ. Các lỗi Bean Validation (`MethodArgumentNotValidException`) hoặc `RuntimeException` ("User đã tồn tại", "Password current is Wrong!"...) sẽ bị Spring trả về format mặc định hoặc mã HTTP 500 kèm chuỗi text thô.
   - Frontend cần parser linh hoạt: đọc cả trường `error`, `message`, hoặc fallback về raw string.
2. **Không hỗ trợ phân trang:**
   - Toàn bộ các API tra cứu (`/api/admin/users`, `/search`, `/api/roles`) trả về mảng đầy đủ `List<T>`, không có phân trang ở backend. Frontend sẽ cần xử lý phân trang phía Client (Client-side pagination).

---

## 4. CHIẾN LƯỢC XỬ LÝ TRÊN FRONTEND (NEXT.JS APP ROUTER)

1. **Token Refresh Interceptor:**
   - Với thời hạn Access Token cực ngắn (6 phút), `api-client.ts` sẽ thiết lập cờ `isRefreshing` và hàng đợi `failedQueue` để gom tất cả request gửi đồng thời khi gặp lỗi 401 do hết hạn token, tránh spam gọi refresh token liên tục.
   - Tuyệt đối không thử refresh nếu request nhận lỗi 403 (Forbidden) hoặc 401 có lý do tài khoản bị khóa/vô hiệu hóa.
2. **Xử lý linh hoạt cho Forgot Password / Reset Password:**
   - Dựng form UI chuẩn chỉ, nhưng khi submit sẽ hiển thị thông báo rõ ràng: *"Chức năng Quên mật khẩu đang được nâng cấp ở hệ thống máy chủ. Vui lòng liên hệ Quản trị viên để được hỗ trợ."* (hoặc kết nối nếu backend bổ sung endpoint).
3. **Client-side Pagination & Filtering cho Admin:**
   - Vì API `/api/admin/users` trả về toàn bộ mảng, FE sẽ hỗ trợ cả 2 chế độ:
     - Gửi query param lên backend khi tìm kiếm theo `username`, `email`, `role`, `status`.
     - Phân trang 10/20/50 bản ghi mỗi trang trực tiếp trên client để UX mượt mà.
