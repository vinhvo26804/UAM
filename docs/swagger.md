# Swagger UI

Khởi động `DemoApplication` từ IDE với cấu hình PostgreSQL hiện tại, hoặc chạy:

```powershell
.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run
```

Lệnh trên bỏ qua biên dịch test vì các test đặt lại mật khẩu hiện đang tham chiếu một số class còn thiếu, như `ResetPasswordRequest` và `PasswordResetService`.

- Swagger UI: http://localhost:8089/swagger-ui.html
- OpenAPI JSON: http://localhost:8089/v3/api-docs
- OpenAPI YAML: http://localhost:8089/v3/api-docs.yaml

Các đường dẫn tài liệu được truy cập không cần đăng nhập.

## Thử API với JWT

1. Mở nhóm **Authentication**, chọn `POST /loginWithJwt` → **Try it out**.
2. Nhập tài khoản đã được xác thực và nhấn **Execute**:

   ```json
   {
     "userName": "your-username",
     "password": "your-password"
   }
   ```

3. Sao chép `accessToken` trong response.
4. Nhấn **Authorize**, dán token vào `bearerAuth` (chỉ token, không thêm `Bearer `), rồi nhấn **Authorize** → **Close**.
5. Thử `GET /me`, `GET /test` hoặc các API có biểu tượng khóa. Swagger UI tự thêm header `Authorization: Bearer <accessToken>`.

Đăng ký, xác thực tài khoản, đăng nhập, refresh token và logout không yêu cầu access token. Với `POST /refreshToken` và `POST /logout`, truyền `refreshToken` trong JSON body. Khi access token hết hạn, dùng `/refreshToken` lấy token mới rồi cập nhật trong **Authorize**.

Cấu hình dùng `springdoc-openapi-starter-webmvc-ui` 3.1.1 cho Spring Boot 4 theo [tài liệu springdoc](https://springdoc.org/).
