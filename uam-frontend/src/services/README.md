# Services

Đặt các hàm gọi API theo chức năng tại đây khi có component sử dụng. Service
gọi helper trong `@/lib/http`, nhận dữ liệu/headers từ caller và trả kết quả cho
component. Component phụ trách nhập liệu, loading và hiển thị lỗi.

Chưa tạo service đăng nhập hay quản lý người dùng vì các trang hiện chỉ là
placeholder. Hợp đồng đã đối chiếu nằm trong `docs/backend-contract.md`.
Không gọi helper URL tương đối này từ Server Component hoặc lúc build.
