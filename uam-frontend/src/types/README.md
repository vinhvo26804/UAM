# Types

Đặt kiểu request/response tại đây khi được service hoặc component sử dụng.
Đối chiếu DTO tại `../src/main/java/com/example/demo/dto` tính từ gốc frontend;
xem danh sách API đã xác minh trong `docs/backend-contract.md`.

Chưa tạo các type nghiệp vụ chưa được sử dụng. Không sao chép Entity `User`,
password hash hoặc thông tin nội bộ của backend. Trường password chỉ thuộc
request cần mật khẩu nếu DTO thực tế yêu cầu. TypeScript không xác thực JSON
tại runtime; kiểm tra dữ liệu và quyền truy cập vẫn thuộc Spring Boot.
