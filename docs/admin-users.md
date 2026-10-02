# UC-15 / UC-16 — Quản trị tài khoản

Các API dưới đây yêu cầu access token của một tài khoản ACTIVE có role ADMIN
trong `user_roles`. Header: `Authorization: Bearer <accessToken>`.

## Trạng thái tài khoản

Java dùng `UserStatus` với `@Enumerated(EnumType.STRING)`. PostgreSQL giữ cột
`VARCHAR` và ràng buộc CHECK, NOT NULL, DEFAULT PENDING. Cách lưu tên enum theo
[Jakarta Persistence](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumtype)
giúp dữ liệu không phụ thuộc vị trí các phần tử enum.

Enum phù hợp vì bốn trạng thái được xác định sẵn và loại trừ nhau. Một boolean
không phân biệt được chưa xác thực, bị khóa và vô hiệu hóa. Chưa cần bảng danh mục
trạng thái riêng hoặc enum native PostgreSQL. Khi thêm/đổi tên trạng thái, cần
migration cập nhật dữ liệu và CHECK tương ứng. Nếu sau này cần giữ trạng thái khóa
độc lập với trạng thái kích hoạt, nên tách thành hai thuộc tính.

| Trạng thái | Ý nghĩa |
| --- | --- |
| PENDING | Đã đăng ký, chưa được xác thực hoặc kích hoạt |
| ACTIVE | Được đăng nhập và truy cập API theo role |
| LOCKED | Bị khóa; khóa thủ công chờ admin mở, khóa do sai mật khẩu có hạn 15 phút |
| DISABLED | Vô hiệu hóa, bao gồm thao tác xóa mềm; chỉ admin kích hoạt lại |

Migration `V5__constrain_user_status.sql` chuyển status null/rỗng cũ về PENDING,
thêm giá trị mặc định và ràng buộc hợp lệ. Migration không đổi trạng thái hợp lệ
hoặc gán role cho tài khoản cũ.

## API

| Method | Đường dẫn | Kết quả |
| --- | --- | --- |
| GET | `/api/admin/users` | Xem danh sách; filter tùy chọn username, email, role, status |
| GET | `/api/admin/users/{id}` | Xem thông tin một user, kể cả user đã vô hiệu hóa |
| PUT | `/api/admin/users/{id}` | Cập nhật email, fullname, doB, phone |
| DELETE | `/api/admin/users/{id}` | Đặt DISABLED, trả 204; giữ user và liên kết role trong DB |
| PATCH | `/api/admin/users/{id}/lock` | ACTIVE → LOCKED, khóa thủ công không tự hết hạn |
| PATCH | `/api/admin/users/{id}/unlock` | LOCKED → ACTIVE |
| PATCH | `/api/admin/users/{id}/disable` | Chuyển tài khoản sang DISABLED |
| PATCH | `/api/admin/users/{id}/activate` | PENDING/DISABLED → ACTIVE |

Ví dụ body PUT:

```json
{
  "email": "updated@example.com",
  "fullname": "Nguyen Van A",
  "doB": "2000-01-15",
  "phone": "0901234567"
}
```

Username là định danh đăng nhập, được giữ nguyên. Nếu gửi trường `username`, giá
trị phải trùng username hiện tại. Role có API quản lý riêng `/api/roles`; password
và status không thuộc body cập nhật hồ sơ. Response không trả password, OTP hoặc
token. Các API PATCH trạng thái không cần body.

Yêu cầu lặp lại trạng thái đích được xử lý an toàn. Không thể dùng unlock để kích
hoạt PENDING/DISABLED, hoặc dùng activate để bỏ qua LOCKED: trường hợp này trả 409.
Khóa một tài khoản đang bị khóa có thời hạn sẽ chuyển thành khóa thủ công.

## Bảo đảm khi chặn tài khoản

JWT filter kiểm tra trạng thái hiện tại trên mỗi request: tài khoản PENDING,
LOCKED hoặc DISABLED bị từ chối dù access token chưa hết hạn. Khóa/vô hiệu hóa
thu hồi các refresh token hiện có; đổi email cũng thu hồi refresh token. Sau khi
mở khóa/kích hoạt, access token còn hạn được xét theo trạng thái mới; refresh token
đã thu hồi không được phục hồi.

OTP đăng ký chỉ được dùng cho PENDING, được xóa sau khi xác thực thành công và
không thể kích hoạt lại tài khoản LOCKED/DISABLED. Khóa do sai mật khẩu được mở
khi đăng nhập sau thời hạn; timer cũ không thể tự kích hoạt tài khoản DISABLED.

Các thay đổi admin được thực hiện trong transaction, khóa bản ghi user để tuần
tự hóa cập nhật với đăng nhập, xác thực OTP, sửa hồ sơ và đổi email/mật khẩu.
Các thao tác tự cập nhật kiểm tra lại trạng thái sau khi lấy khóa; request đã qua
JWT filter vẫn bị từ chối nếu admin vừa khóa/vô hiệu hóa user. Không có lệnh xóa
user khỏi DB.

`spring.jpa.open-in-view=false` tránh giữ entity cũ từ JWT filter đến lúc cập nhật.
Các response hồ sơ được tạo trong transaction; truy vấn role tải permissions
trước khi trả kết quả, không serialize liên kết ngược từ permission về role.

## Mã HTTP

- 400: dữ liệu không hợp lệ.
- 401: thiếu/sai/hết hạn token, hoặc tài khoản không hoạt động.
- 403: tài khoản không có ADMIN.
- 404: không tìm thấy user.
- 409: email trùng, yêu cầu đổi username, hoặc chuyển trạng thái không hợp lệ.
- 423: đăng nhập tài khoản đang bị khóa.

Swagger UI: `http://localhost:8089/swagger-ui.html`, nhóm **Admin Users**.

## Kiểm thử

Ngày 2026-10-01: 155 test liên quan đạt (137 test logic/MVC/security và 18 test
PostgreSQL). Các test DB dùng user thử nghiệm và rollback; xác nhận giữ bản ghi
user, liên kết role, thu hồi refresh token, chặn JWT theo trạng thái và phục hồi
truy cập sau unlock/activate. Migration V5 được kiểm tra trên bảng tạm PostgreSQL,
chưa áp dụng lên bảng `users` hiện hữu trong lần kiểm thử này.

`mvn test` toàn bộ hiện bị chặn ở bước biên dịch vì các test reset mật khẩu tham
chiếu class còn thiếu (`PasswordResetService`, `ResetPasswordRequest`,
`PasswordResetOtp`, `PasswordResetOtpRepository`, `PasswordResetController`).
POM kiểm thử tạm chỉ biên dịch nhóm liên quan, không thay đổi POM chính:

```powershell
.\mvnw.cmd -o -f target/uc15-16-verification/pom.xml '-Dspring.flyway.enabled=false' test
```

Lệnh trên dùng được khi POM tạm còn tồn tại. Log kết quả:
`target/uc15-16-verification/unit-tests.log` và
`target/uc15-16-verification/persistence-tests.log`.
