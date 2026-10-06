Hãy rà soát toàn bộ source frontend UAM và hoàn thiện logic FE để các chức năng hiện có hoạt động nhất quán, giảm lỗi phát sinh khi kiểm thử.
Thực hiện trực tiếp trong workspace. Không chỉ đưa nhận xét hoặc kế hoạch.
- Chỉ chỉnh sửa source, cấu hình, tài liệu và test thuộc frontend.
- Được đọc backend Spring Boot, Controller, DTO, SecurityConfig và OpenAPI để hiểu API contract.
- Không sửa backend, database, migration hoặc quy tắc nghiệp vụ backend.
- Nếu vấn đề cần backend thay đổi, ghi rõ bằng chứng và giới hạn; không tự sửa hoặc tạo kết quả thành công giả ở FE.
- Đọc hướng dẫn repository và kiểm tra các thay đổi đang có. Giữ nguyên công việc chưa commit của người dùng.
Rà soát đầy đủ:
- Routing, layout, page và component.
- Form, validation và trạng thái giao diện.
- Services, HTTP client và request/response types.
- Store, hook, context và cơ chế lưu phiên.
- Login, logout, refresh token và phân quyền.
- Cách đọc/ghi browser storage và đồng bộ giữa các component/tab.
- Error handling và các test hiện có.
Lập bản đồ từng luồng:
Thao tác người dùng → component → state/store → service → endpoint → xử lý response → cập nhật giao diện.
Đối chiếu endpoint và DTO với backend thực tế. Không suy đoán từ tên hàm hoặc thông tin cũ trong hội thoại.
Không chỉ vá tại component đầu tiên báo lỗi. Tìm nguyên nhân trong hook, store hoặc HTTP client và kiểm tra tất cả nơi sử dụng.
Ưu tiên:
- Snapshot của useSyncExternalStore ổn định khi dữ liệu chưa thay đổi.
- Snapshot được cập nhật và subscriber được thông báo khi dữ liệu thực sự thay đổi.
- Không có vòng lặp render, effect, subscription hoặc API.
- Không gọi setState trong render.
- Dependency của effect đúng và cleanup đầy đủ.
- Không truy cập window/localStorage trong server rendering.
- Server snapshot và client hydration nhất quán.
- Đăng nhập/đăng xuất cập nhật Header, sidebar và các trang ngay trong cùng tab.
- Đồng bộ các tab nếu cơ chế lưu phiên hiện tại yêu cầu.
- Không coi React Strict Mode là nguyên nhân để tắt nhằm che lỗi.
Tái sử dụng cấu trúc hiện có. Chỉ thêm file hoặc abstraction khi có trách nhiệm rõ ràng.
Theo hợp đồng backend hiện có:
- Gửi đúng method, URL, headers, query và body.
- Phân biệt endpoint public và endpoint cần token.
- Phân biệt lỗi chưa xác thực, thiếu quyền, validation và lỗi server.
- Đọc được response JSON, text và response rỗng.
- Không ép tất cả response về một kiểu hoặc coi mọi lỗi là phiên hết hạn.
- Không tự động retry vô hạn.
Với refresh token:
- Chỉ refresh khi có bằng chứng phù hợp rằng access token không còn hợp lệ và backend hỗ trợ refresh.
- Các request đồng thời dùng chung một lần refresh.
- Request refresh không tự kích hoạt refresh tiếp.
- Request gốc thử lại tối đa một lần sau refresh.
- Không refresh cho lỗi thiếu quyền 403.
- Logout hoặc refresh thất bại phải xử lý state nhất quán.
- Request hoàn thành muộn không được khôi phục phiên đã logout.
Giữ cơ chế lưu token phù hợp với backend hiện tại. Không tự chuyển token sang localStorage hoặc thay đổi mô hình xác thực để tiện triển khai.
Kiểm tra tất cả chức năng đã có:
- Login, register và các bước OTP.
- Forgot/reset password.
- Hồ sơ, đổi mật khẩu và đổi email.
- Danh sách, chi tiết và thao tác quản lý user.
- Role, trạng thái tài khoản, phiên đăng nhập nếu đã được backend hỗ trợ.
Với mỗi luồng:
- Validate theo DTO và quy tắc backend.
- Có loading, success, error và empty state phù hợp.
- Chặn submit lặp trong khi request đang chạy.
- Khi lỗi, giữ dữ liệu form cần thiết để người dùng sửa.
- Chỉ hiển thị thành công sau khi API thành công.
- Sau thao tác ghi, cập nhật/refetch dữ liệu liên quan để tránh giao diện cũ.
- Reset trạng thái đúng khi đổi user, đổi trang hoặc mở lại modal.
- Xử lý race condition trong tìm kiếm và tải dữ liệu.
- Phân trang/lọc giữ trạng thái nhất quán nếu chức năng đã có.
- Menu và điều hướng theo quyền; backend vẫn quyết định quyền truy cập.
Không thiết kế lại toàn bộ giao diện hoặc thêm nghiệp vụ mới ngoài phạm vi rà soát.
Chạy các kiểm tra phù hợp:
- ESLint.
- TypeScript.
- Production build.
- Các test hiện có.
- Kiểm tra trình duyệt ở desktop và mobile.
Bổ sung test có ý nghĩa cho logic quan trọng vừa sửa, đặc biệt:
- Snapshot ổn định và cập nhật đúng.
- Đăng nhập/đăng xuất đồng bộ giao diện.
- Refresh đồng thời chỉ gửi một request.
- Refresh thất bại không tạo vòng lặp.
- Response rỗng và lỗi API được xử lý đúng.
- Request cũ không ghi đè state mới.
Không tạo test chỉ lặp lại chi tiết triển khai. Tái sử dụng công cụ test hiện có; không thêm framework lớn nếu chưa cần.
Nếu có backend và tài khoản test, kiểm tra các luồng thành công, dữ liệu sai, chưa đăng nhập và thiếu quyền. Chỉ dùng dữ liệu test phù hợp; không thay đổi tài khoản thật để kiểm tra.
Nếu thiếu điều kiện kiểm tra, ghi rõ phần chưa xác minh. Mock chỉ được dùng trong test, không thay response thật trong ứng dụng.
- Các kiểm tra đã chạy phải có kết quả rõ ràng.
- Không còn lỗi console chưa giải thích trong các luồng đã kiểm tra.
- Không còn vòng lặp render hoặc request.
- Các chức năng dùng chung store/HTTP client vẫn hoạt động sau sửa.
- Không có thay đổi nào thuộc backend.
- Không che lỗi bằng catch rỗng, dữ liệu giả hoặc thông báo thành công mặc định.
Rà lại diff cuối cùng để xác nhận đúng phạm vi và không làm mất thay đổi của người dùng.
Trả về ngắn gọn bằng tiếng Việt:
1. Nguyên nhân chính và cách đã sửa.
2. Tạo 1 folder Error, Ghi lại tất cả lỗi đã lặp phải và không bao giờ lặp lại lỗi đó
3. Các file FE đã thay đổi.
4. Bảng chức năng và trạng thái kiểm chứng.
5. Kết quả lint, TypeScript, build và test.
6. Lỗi hoặc giới hạn phụ thuộc backend, kèm endpoint và bằng chứng.
7. Các bước kiểm tra thủ công còn cần người dùng thực hiện.
Tiếp tục làm đến khi hoàn thành phần FE có thể xử lý trong phạm vi này. Không deploy, không tự commit/push.