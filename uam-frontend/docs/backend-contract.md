# Đối chiếu API backend

Tài liệu dựa trên source Spring Boot hiện có, chưa xác nhận response bằng request thực tế. Giai đoạn này chỉ dựng nền tảng FE; các trang nghiệp vụ hiển thị “Chưa triển khai”.

## Kết nối và xác thực

- `server.port` là `8089` trong [application.yaml](../../src/main/resources/application.yaml); `.env.local` dùng `BACKEND_URL=http://localhost:8089` khi backend chạy tại máy này.
- Trình duyệt gọi `/backend/...`; Next.js chuyển tiếp sang backend. Helper URL tương đối trong `src/lib/http.ts` dành cho phía trình duyệt.
- [SecurityConfig](../../src/main/java/com/example/demo/config/SecurityConfig.java) cấu hình stateless; [JwtAuthenticationFilter](../../src/main/java/com/example/demo/config/JwtAuthenticationFilter.java) nhận `Authorization: Bearer <accessToken>`, kiểm tra loại token `access` và trạng thái tài khoản hiện tại.
- Không có cơ chế cookie trong hợp đồng đã đọc. FE chưa lưu token, refresh token tự động hoặc bảo vệ route theo phiên đăng nhập.
- `GET /v3/api-docs` được cho phép công khai, phù hợp kiểm tra proxy bằng thao tác chỉ đọc. `GET /test` yêu cầu xác thực.
- Backend vẫn chịu trách nhiệm nghiệp vụ và quyền truy cập; layout FE chưa xác thực người dùng.

## Endpoint đã thấy trong controller

Đường dẫn dưới đây là đường dẫn backend, chưa có tiền tố `/backend`. Body được biểu diễn bằng tên trường JSON, không phải dữ liệu mẫu thật.

| Method và đường dẫn | Request | Response thành công / quyền |
| --- | --- | --- |
| POST `/loginWithJwt` | `{userName, password}` | `LoginResponse`, công khai |
| POST `/api/v1/auth/register` | `{userName, password, email}` | Text `Register Success`, công khai |
| POST `/api/v1/auth/verify` | `{userName, otpCode}` | Text `Verify Success`, công khai |
| POST `/refreshToken` | `{refreshToken}` | `LoginResponse`, công khai |
| POST `/logout` | `{refreshToken}` | `204`, không body, công khai |
| PUT `/changePassword` | `{currentPassword, newPassword, confirmNewPassword}` | Text, access token |
| GET `/me` | Không body | `UserResponse`, access token |
| PUT `/me/update` | `{fullname, doB, phone}` | Text, access token |
| PUT `/me/email` | `{email}` | Text, access token |
| POST `/me/email/verify` | `{otpCode}` | Text, access token |
| GET `/search` | Query tùy chọn `username`, `email`, `role`, `status` | `UserResponse[]`, ADMIN |
| GET `/api/admin/users` | Query tùy chọn như `/search` | `AdminUserResponse[]`, ADMIN |
| GET `/api/admin/users/{id}` | ID trong path | `AdminUserResponse`, ADMIN |
| PUT `/api/admin/users/{id}` | `{email, fullname, doB, phone, username?}` | `AdminUserResponse`, ADMIN |
| DELETE `/api/admin/users/{id}` | Không body | `204`, xóa mềm, ADMIN |
| PATCH `/api/admin/users/{id}/lock` | Không body | `AdminUserResponse`, ADMIN |
| PATCH `/api/admin/users/{id}/unlock` | Không body | `AdminUserResponse`, ADMIN |
| PATCH `/api/admin/users/{id}/disable` | Không body | `AdminUserResponse`, ADMIN |
| PATCH `/api/admin/users/{id}/activate` | Không body | `AdminUserResponse`, ADMIN |
| GET `/api/roles` | Không body | Mảng Role, ADMIN |
| GET `/api/roles/{roleName}` | Tên role trong path | Role, ADMIN |
| POST `/api/roles/assign` | `{roleName, username}` | Text, ADMIN |
| DELETE `/api/roles/remove` | `{roleName, username}` | Text, ADMIN |

Nguồn route: [AuthController](../../src/main/java/com/example/demo/controller/AuthController.java), [UserController](../../src/main/java/com/example/demo/controller/UserController.java), [AdminUserController](../../src/main/java/com/example/demo/controller/AdminUserController.java), [RoleController](../../src/main/java/com/example/demo/controller/RoleController.java). Quyền ADMIN đặt tại [AdminUserService](../../src/main/java/com/example/demo/service/AdminUserService.java), [UserService](../../src/main/java/com/example/demo/service/UserService.java) và [RoleService](../../src/main/java/com/example/demo/service/RoleService.java).

## DTO và tên trường cần giữ chính xác

| DTO | Trường từ source |
| --- | --- |
| [LoginRequest](../../src/main/java/com/example/demo/dto/request/LoginRequest.java) | `userName: String`, `password: String` |
| [LoginResponse](../../src/main/java/com/example/demo/dto/response/LoginResponse.java) | `accessToken: String`, `refreshToken: String`, `tokenType: String` (constructor đặt `Bearer`) |
| [UserResponse](../../src/main/java/com/example/demo/dto/response/UserResponse.java) | `id: long`, `username: String`, `phone: String`, `dob: LocalDate`, `status: UserStatus`, `fullname: String`, `roles: Set<String>`; không có email |
| [AdminUserResponse](../../src/main/java/com/example/demo/dto/response/AdminUserResponse.java) | `id: Long`, `username: String`, `email: String`, `fullname: String`, `doB: LocalDate`, `phone: String`, `status: UserStatus`, `roles: Set<String>`, `lockUntill: LocalDateTime` |
| [UpdateUserRequest](../../src/main/java/com/example/demo/dto/request/User/UpdateUserRequest.java) | `fullname: String`, `doB: LocalDate`, `phone: String` |
| [AdminUpdateUserRequest](../../src/main/java/com/example/demo/dto/request/User/AdminUpdateUserRequest.java) | `username` tùy chọn và phải trùng username hiện tại; `email`, `fullname` bắt buộc; `doB`, `phone` không có ràng buộc bắt buộc |

`userName` của auth khác `username` trong response. `UserResponse` dùng `dob`, trong khi request cập nhật và admin response dùng `doB`; `lockUntill` giữ nguyên cách viết của backend. [UserStatus](../../src/main/java/com/example/demo/entity/UserStatus.java) gồm `PENDING`, `ACTIVE`, `LOCKED`, `DISABLED`. Các ngày dùng kiểu Java như bảng; cần xác nhận định dạng JSON khi tích hợp nghiệp vụ. `Set<String>` được biểu diễn thành mảng trong JSON.

DTO response không khai báo đầy đủ ràng buộc null. Đăng ký chưa yêu cầu `fullname`, `phone`, ngày sinh, và service đặt `lockUntill` về null trong một số nhánh; không giả định mọi trường hồ sơ luôn có giá trị. Chưa thêm type nghiệp vụ chưa dùng, và không sao chép Entity User chứa password/hash sang FE.

## Phần cần đối chiếu trước khi làm nghiệp vụ

- Chưa có controller cho luồng quên/đặt lại mật khẩu trong source hiện tại. `ForgotPasswordRequest` và method `forgotPassword` rời trong [AuthService](../../src/main/java/com/example/demo/service/AuthService.java) không xác lập endpoint; không tự đặt URL cho trang `/forgot-password`.
- `/api/v1/auth/login` chỉ xuất hiện trong code comment; endpoint đăng nhập đang khai báo là `/loginWithJwt`.
- Cần chọn cơ chế quản lý phiên và lưu token cho FE trước khi triển khai đăng nhập. Source refresh hiện trả access token mới cùng refresh token cũ; logout nhận refresh token trong JSON.
- Error response chưa thống nhất: Auth/Admin có nhánh `{error: string}`, nhưng validation, lỗi mặc định và `RuntimeException` có thể có body khác. HTTP helper phải giữ status và body JSON/text để component xử lý phù hợp.
- Xác thực OTP có nhánh thất bại vẫn trả HTTP 200 với text như `Verify failed` hoặc `OTP expired!`; không coi mọi `response.ok` là thành công nghiệp vụ.
- Đăng ký tạo/lưu OTP nhưng chưa thấy gửi email trong method; đổi email đang in OTP ra log. Cần xác định kênh nhận OTP trước khi hoàn thiện form.
- Phải đối chiếu nullability, định dạng ngày và response thực tế khi tích hợp từng chức năng; backend đang chạy có thể chưa đồng bộ với source đã đọc.
