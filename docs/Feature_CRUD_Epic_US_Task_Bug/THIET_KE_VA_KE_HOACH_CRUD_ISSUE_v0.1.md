# Thiết kế và kế hoạch CRUD Issue — v0.1

Ngày: 06/10/2026. Trạng thái: **APPROVED** (Đã được người dùng phê duyệt toàn bộ phạm vi CẢ HAI và các đề xuất D-01…D-10).

Tài liệu kế hoạch này đã được xác nhận và là cơ sở triển khai chính thức cho Bước 6 — Phát triển chức năng. Tuân thủ nghiêm ngặt nguyên tắc: tái sử dụng UAM/Team hiện có, không can thiệp chức năng ngoài phạm vi, kiểm tra đầy đủ quyền và tính toàn vẹn dữ liệu.

## 1. Xác nhận yêu cầu, phạm vi và hiện trạng

### 1.1. Nguồn và điểm dừng

- Quy trình: [FEATURE_DEVELOPMENT_WORKFLOW.md](../FEATURE_DEVELOPMENT_WORKFLOW.md), bước 1–8.
- BA: [Phan-tich-dac-ta-Issue-Epic-Sprint.md](Phan-tich-dac-ta-Issue-Epic-Sprint.md), phiên bản 1.0, ngày 06/10/2026; giữ nguyên mã BR-01…BR-20, US-01…US-06, AC-01…AC-26.
- Định hướng thiết kế: [TaiLieuThietKeHeThong.md](TaiLieuThietKeHeThong.md).
- Hướng dẫn FE: [AGENTS.md](../../uam-frontend/AGENTS.md).

Hai tài liệu đầu vào trong thư mục tính năng mang đuôi `.md` nhưng nội dung là Word OpenXML. Đã đọc phần văn bản và bảng từ `word/document.xml`; không đổi định dạng hoặc ghi đè bản gốc.

Tình trạng các bước:
- **Bước 1–5: ĐÃ HOÀN THÀNH VÀ ĐƯỢC PHÊ DUYỆT.**
- **Bước 6: Đang triển khai code theo thiết kế.**

### 1.2. Phạm vi đã được duyệt

Phạm vi chính thức: **CẢ HAI (Fullstack Frontend + Backend)**:

| Nhóm | Nội dung triển khai | Trạng thái |
| --- | --- | --- |
| I — Issue | CRUD chung cho Epic, Story (US), Task, Bug; xem, lọc, phân trang, sửa thông tin, xóa mềm | Đã duyệt |
| H — Cha/con | Subtask, liên kết Epic, chuyển trạng thái và mở lại theo invariant | Đã duyệt |
| S — Sprint | Tạo Sprint rỗng, bắt đầu Sprint, liên kết Issue, preview và Complete Sprint, lịch sử chuyển | Đã duyệt |
| F — Space | Space riêng, thành viên và quyền riêng (ADMIN, MANAGER, MEMBER, VIEWER) | Đã duyệt |

Ngoài phạm vi phiên bản này: chuyển Space của Issue, đổi IssueType sau tạo, chuyển cha Subtask, Subtask lồng nhau, Epic lồng nhau, custom IssueType/workflow, bulk API, kéo thả board, báo cáo/burndown, comment, attachment, email/notification, lưu nháp tự động, khôi phục/xóa vĩnh viễn, reopen Sprint đã CLOSED.
