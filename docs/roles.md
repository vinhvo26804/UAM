# Role mặc định và quyền search

`AuthService.register()` gán `USER` khi đăng ký. Quan hệ được lưu trong
`user_roles(user_id, role_id)` và tên quyền nằm ở bảng `role`; cột cũ
`users.role` không được entity `User` sử dụng.

`CustomUserDetailsService` chuyển `ADMIN` trong DB thành authority `ROLE_ADMIN`.
`GET /search` yêu cầu `hasRole('ADMIN')`, do đó tài khoản chỉ có `USER` vẫn nhận
403. Quy tắc tiền tố được mô tả trong
[Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).

## Kiểm tra quyền đang được ứng dụng sử dụng

```sql
SELECT u.id, u.username, r.name AS role_name
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN role r ON r.id = ur.role_id
ORDER BY u.id, r.name;
```

Sau khi khởi động lại ứng dụng với code mới, đăng nhập và dùng access token cho
`GET /me` để xem `roles`, rồi gọi `GET /search`. Nếu sửa quyền khi ứng dụng đang
chạy, JWT filter đọc lại quyền DB trên mỗi request; token còn hạn sẽ nhận quyền
mới ngay. Trong Swagger, dán access token vào **Authorize**.

Quản lý quyền qua API `/api/roles` bằng một tài khoản đã có ADMIN, hoặc chỉnh
`user_roles`. Thay đổi `users.role` sẽ không thay đổi quyền ứng dụng. Migration V4
chỉ tạo danh mục USER/ADMIN/MODERATOR, không gán quyền cho tài khoản đã tồn tại.

## Đề xuất sửa dữ liệu cũ — chưa áp dụng

Kiểm tra DB local ngày 2026-10-01 thấy 4 tài khoản đã có USER trong `user_roles`
và 11 tài khoản chưa có liên kết quyền. Tài khoản ID 33 có `users.role = 'ADMIN'`
nhưng chưa có ADMIN trong `user_roles`.

Phạm vi cần duyệt trước khi sửa DB:

| Tài khoản | Liên kết cần thêm |
| --- | --- |
| ID 1–10 | USER nếu tài khoản vẫn chưa có quyền |
| ID 33 | USER nếu tài khoản vẫn chưa có quyền; ADMIN nếu cột cũ vẫn là ADMIN |

Các liên kết đang có được giữ nguyên. Trước khi thực hiện phải kiểm tra lại các
điều kiện trên và dùng một transaction; không tự suy đoán quyền cho tài khoản khác.

Bộ duyệt tự động đã từ chối migration sửa quyền hàng loạt. Chưa có migration mới
hoặc cập nhật quyền cho các tài khoản hiện hữu được áp dụng.

## Kiểm thử

`RegistrationPersistenceIntegrationTests` kiểm tra đăng ký ba trường bắt buộc,
lưu USER vào bảng liên kết, mật khẩu mã hóa và OTP trên PostgreSQL; dữ liệu test
được rollback. `AdminSearchSecurityTests` kiểm tra ADMIN được search, các role khác
nhận 403, thiếu token nhận 401 và cùng token phản ánh thay đổi quyền.

Các test password reset hiện tham chiếu class còn thiếu, nên `mvn test` toàn bộ
chưa biên dịch được. Các test liên quan được chạy riêng bằng POM kiểm thử tạm trong
`target/role-verification/pom.xml`, không thay đổi POM chính.

Kết quả ngày 2026-10-01: 41 test liên quan đạt, không có failure/error. Chạy lại
bộ kiểm thử này bằng `./mvnw.cmd -f target/role-verification/pom.xml test` khi POM
tạm còn tồn tại. Log: `target/role-verification/test.log`.

`AdminSearchPersistenceIntegrationTests` kiểm tra `/search` qua security và
PostgreSQL thật, với filter null/rỗng, lọc role và các bộ lọc không phân biệt hoa
thường. Truy vấn dùng `CAST(:filter AS String)` để tránh lỗi `lower(bytea)` khi
tham số null; cú pháp cast theo
[tài liệu Hibernate HQL](https://docs.jboss.org/hibernate/orm/7.0/querylanguage/html_single/Hibernate_Query_Language.html#functions-typecasts).
