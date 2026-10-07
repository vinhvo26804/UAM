# BÁO CÁO ĐÁNH GIÁ TOÀN DIỆN & KẾ HOẠCH XỬ LÝ (PO & END-USER AUDIT)
**Dự án**: Jira Clone (Spring Boot 4.1.0 + Next.js 16 + PostgreSQL 18.1)  
**Người thực hiện**: Product Owner & End-User Auditor  
**Thời gian thực hiện**: 07/10/2026  
**Môi trường kiểm thử**: 
- OS: Windows 11
- Browser: Microsoft Edge (Chromium Engine, Headless/Headed Automation via Playwright)
- Backend: Spring Boot 4.1.0 (Java 24, Tomcat 8089)
- Frontend: Next.js 16 (React 19, Turbopack, Port 3000)
- Database: PostgreSQL 18.1 (Flyway Schema v7)
- Tài khoản kiểm thử: `po_tester` (Space Admin / Project Owner), `po_member` (Member)

---

## 1. ĐÁNH GIÁ TỔNG QUAN

**Kết luận hiện trạng**: **DÙNG ĐƯỢC MỘT PHẦN (Partially Usable)**

### Phân tích đánh giá:
1. **Điểm đạt được (Strengths)**:
   - Các luồng khởi tạo không gian (**Space**), phân quyền thành viên (**SpaceMember**) và các quy tắc khởi tạo Issue cơ bản (Epic, User Story, Task, Subtask) hoạt động tốt trên giao diện UI.
   - Các ràng buộc bất biến cốt lõi (Core Business Invariants) ở tầng backend hoạt động rất chặt chẽ:
     - **BR-11 / AC-10**: Chặn cha chuyển sang `DONE` khi còn Subtask chưa hoàn thành (`OPEN_SUBTASKS` - HTTP 409).
     - **BR-12 / AC-13**: Chặn mở lại Subtask khi cha đã `DONE` (`PARENT_DONE` - HTTP 409) và ẩn nút "+ Thêm Subtask" trên UI khi cha ở trạng thái `DONE`/`CANCELLED`.
     - Chặn tạo Subtask trực tiếp gắn vào Epic hoặc không có cha hợp lệ.
     - Luồng vòng đời Sprint (Tạo Sprint, Bắt đầu Sprint `FUTURE` -> `ACTIVE`, xem trước kết quả đóng Sprint) hoạt động đúng logic.
2. **Điểm nghẽn nghiêm trọng (Critical Blockers)**:
   - **LỖI TÍNH NĂNG UPDATE**: Giao diện chi tiết công việc (`/spaces/[spaceId]/issues/[issueId]`) **hoàn toàn không có nút hoặc form chỉnh sửa thông tin Issue** (Summary, Description, Priority, Assignee, Estimate, Due Date). Mặc dù Backend API (`PATCH /api/v1/issues/{id}`) và Frontend Service đã được viết, nhưng UI đã bỏ sót toàn bộ tính năng này.
   - **LỖI TRUY VẤN SQL POSTGRESQL (Crash 500)**: Khi mở màn hình chi tiết Epic (hoặc Task có subtask) để tải danh sách công việc con, frontend gọi API tìm kiếm không kèm từ khóa (`q=null`). Câu truy vấn JPQL trong `IssueRepository.java` sinh ra câu lệnh SQL `LOWER(bytea)` trên PostgreSQL, khiến database văng ngoại lệ `42883: function lower(bytea) does not exist` và trả về `500 Internal Server Error`, làm trang chi tiết báo lỗi đỏ.
   - **XÓA CÔNG VIỆC**: Popup xóa công việc chưa thể hiện tên hoặc Issue Key cụ thể để người dùng kiểm chứng trước khi xóa vĩnh viễn/mềm.

---

## 2. MA TRẬN KIỂM THỬ (TEST MATRIX)

| Đối tượng | Thao tác (Action) | Trạng thái | Bằng chứng kiểm thử (Evidence) |
| :--- | :--- | :---: | :--- |
| **Epic** | **Create** | **PASS** | Tạo Epic thành công qua form `/spaces/5/issues/new`. Dữ liệu lưu đúng `issue_type='EPIC'`, không cho phép gán sprint hay cha. Key `POTEST-1` sinh tự động. |
| **Epic** | **Read** | **FAIL** | Truy cập `/spaces/5/issues/7`. API `GET /api/v1/issues?spaceId=5&epicId=7` văng lỗi 500 do lỗi `lower(bytea) does not exist`. Giao diện báo lỗi "Internal Server Error" thay vì hiển thị danh sách issue con. |
| **Epic** | **Update** | **FAIL** | Giao diện chi tiết Epic hoàn toàn không có nút "Chỉnh sửa" / Edit form. Người dùng không thể cập nhật tóm tắt, mô tả hay người phụ trách trên UI. |
| **Epic** | **Delete** | **PASS** | Khi Epic chưa có công việc con, nút "Xóa công việc" hiển thị modal xác nhận và xóa mềm thành công (`deleted_at` được ghi nhận). Khi có con, backend chặn với mã lỗi `ISSUE_HAS_CHILDREN`. |
| **User Story** | **Create** | **PASS** | Tạo User Story liên kết Epic thành công. Form hiển thị dropdown chọn Epic tương ứng. |
| **User Story** | **Read** | **PASS** | Chi tiết User Story hiển thị đầy đủ thông tin tóm tắt, mô tả, thẻ Epic liên kết (`POTEST-1`). |
| **User Story** | **Update** | **FAIL** | Không có nút "Chỉnh sửa" trên UI. Người dùng chỉ có thể đổi trạng thái (TODO -> IN_PROGRESS -> DONE), không sửa được nội dung. |
| **User Story** | **Delete** | **PASS** | Modal xác nhận hiển thị và thực hiện xóa mềm thành công nếu không thuộc Sprint đang Active. |
| **Task** | **Create** | **PASS** | Tạo Task độc lập hoặc gắn Epic thành công. |
| **Task** | **Read** | **FAIL** | Khi Task có Subtask, việc truy vấn danh sách Subtask bị lỗi HTTP 500 do câu lệnh `searchIssues` bị lỗi PostgreSQL `lower(bytea)`. |
| **Task** | **Update** | **FAIL** | Không có nút chỉnh sửa thông tin Task trên UI. |
| **Task** | **Delete** | **PASS** | Chặn xóa thành công nếu Task còn Subtask chưa xóa (`ISSUE_HAS_CHILDREN`). Cho phép xóa khi không còn Subtask. |
| **Subtask** | **Create** | **PASS** | Tạo Subtask thành công từ trang tạo issue với `parentIssueId`. Ràng buộc cha phải là Story/Task/Bug hoạt động đúng. |
| **Subtask** | **Read** | **PASS** | Hiển thị thông tin Subtask và liên kết dẫn về Issue cha. |
| **Subtask** | **Update** | **FAIL** | Không có nút chỉnh sửa trên UI. |
| **Subtask** | **Delete** | **PASS** | Xóa Subtask thành công thông qua modal xác nhận. |
| **Status Transition (BR-11)** | **Execute** | **PASS** | Task có Subtask ở `TODO` -> Bấm chuyển Task sang `DONE` -> Bị chặn với thông báo lỗi: *"Không thể chuyển sang hoàn thành khi còn công việc con chưa xong"* (HTTP 409 `OPEN_SUBTASKS`). |
| **Sprint Lifecycle** | **Create/Start** | **PASS** | Tạo Sprint `Sprint 01` và bấm "Bắt đầu Sprint" -> Trạng thái chuyển thành `ACTIVE`. Chặn tạo đồng thời 2 Active sprint. |

---

## 3. DANH SÁCH LỖI CHI TIẾT (DEFECT DETAILS)

### BUG-01: Giao diện chi tiết Issue hoàn toàn thiếu tính năng Chỉnh sửa (Missing Edit Issue UI)
- **Phân loại**: Chức năng (Functional) / UI-UX
- **Mức độ**: **Critical**
- **Môi trường**: Tất cả trình duyệt, môi trường Web Next.js 16
- **Tiền điều kiện**: Đã tạo thành công bất kỳ Issue nào (Epic, Story, Task, Subtask).
- **Các bước tái hiện**:
  1. Đăng nhập vào hệ thống.
  2. Truy cập vào Không gian dự án bất kỳ.
  3. Mở xem chi tiết một Issue tại URL: `/spaces/[spaceId]/issues/[issueId]`.
  4. Quan sát toàn bộ khu vực header, thân trang và cột bên phải.
- **Kết quả mong đợi**: Có nút "Chỉnh sửa" (Edit) hoặc cho phép chỉnh sửa trực tiếp (inline edit) các trường: Tóm tắt (Summary), Mô tả (Description), Độ ưu tiên (Priority), Người được giao (Assignee), Giờ ước lượng (Estimate Hours), Hạn chót (Due Date), Nhãn (Labels).
- **Kết quả thực tế**: Trang chỉ có các nút chuyển trạng thái (TODO, IN_PROGRESS, DONE...) và nút "Xóa công việc". Người dùng hoàn toàn không có cách nào chỉnh sửa nội dung Issue từ giao diện web.
- **Bằng chứng**: 
  - File `uam-frontend/src/app/(workspace)/spaces/[spaceId]/issues/[issueId]/page.tsx` hoàn toàn không có `useState` cho chế độ chỉnh sửa, không có nút Edit, và không gọi `issueService.updateIssue`.
- **Nguyên nhân kỹ thuật**: Đội ngũ phát triển Frontend chỉ xây dựng trang `page.tsx` ở chế độ xem (View mode) và chuyển trạng thái, chưa thiết kế Modal hoặc Form Edit tương ứng với API `PATCH /api/v1/issues/{id}` đã có sẵn ở backend.
- **Hướng xử lý**: Bổ sung nút "Chỉnh sửa" trên thanh thao tác của trang chi tiết issue, mở Modal chỉnh sửa hoặc kích hoạt Form chỉnh sửa có đầy đủ các trường hợp lệ theo vai trò người dùng và gọi `issueService.updateIssue(id, payload)`.
- **Phạm vi ảnh hưởng**: Trang chi tiết Issue của toàn bộ các loại công việc (Epic, Story, Task, Bug, Subtask).
- **Cách kiểm tra sau sửa**: Mở chi tiết issue -> Bấm "Chỉnh sửa" -> Thay đổi Summary, Description, Priority -> Bấm "Lưu" -> Kiểm tra dữ liệu được cập nhật trên giao diện và trong Database.

---

### BUG-02: Lỗi HTTP 500 khi tra cứu Issue/Subtask/Epic do truy vấn PostgreSQL `lower(bytea)`
- **Phân loại**: Nghiệp vụ (Business Query) / Kỹ thuật Backend
- **Mức độ**: **Blocker**
- **Môi trường**: PostgreSQL 18.1, Spring Boot 4.1.0 (Hibernate 7.4.1.Final)
- **Tiền điều kiện**: Tạo một Epic hoặc Task, sau đó mở trang chi tiết để xem các công việc con.
- **Các bước tái hiện**:
  1. Tạo một Epic (ví dụ ID 7).
  2. Truy cập vào trang chi tiết: `/spaces/5/issues/7`.
  3. Quan sát giao diện và Network tab trong DevTools.
- **Kết quả mong đợi**: Trang hiển thị thông tin Epic và danh sách các Issue con thuộc Epic (nếu có).
- **Kết quả thực tế**: Màn hình hiển thị khung cảnh báo đỏ với nội dung `"Internal Server Error"`. Console báo lỗi: `Failed to load resource: the server responded with a status of 500 (Internal Server Error)`.
- **Bằng chứng**:
  - Log Spring Boot (`task-314.log`):
    ```text
    org.postgresql.util.PSQLException: ERROR: function lower(bytea) does not exist
      Hint: No function matches the given name and argument types. You might need to add explicit type casts.
      Position: 473
    org.springframework.dao.InvalidDataAccessResourceUsageException: JDBC exception executing SQL [ERROR: function lower(bytea) does not exist]
      at com.example.demo.respository.IssueRepository.searchIssues
    ```
- **Nguyên nhân kỹ thuật**: Trong `IssueRepository.java`, câu truy vấn JPQL:
  ```java
  (:q IS NULL OR LOWER(i.summary) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.issueKey) LIKE LOWER(CONCAT('%', :q, '%')))
  ```
  Khi client không truyền tham số tìm kiếm `q` (ví dụ khi tải subtasks hoặc epic children: `searchIssues({ spaceId, epicId })`), `:q` có giá trị `null`. Hibernate 7 khi kết hợp với PostgreSQL JDBC Driver không suy luận được kiểu dữ liệu của biến `:q` null nên truyền kiểu mặc định là `bytea`. PostgreSQL không có hàm `LOWER(bytea)` dẫn đến lỗi truy vấn và trả về HTTP 500.
- **Hướng xử lý**:
  - Cách 1 (Khuyên dùng): Ép kiểu tường minh trong JPQL: `(CAST(:q AS string) IS NULL OR LOWER(i.summary) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(i.issueKey) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))`
  - Cách 2: Trong `IssueService.searchIssues`, nếu `request.getQ()` rỗng hoặc null thì dùng câu truy vấn riêng không có mệnh đề `LOWER(:q)`.
- **Phạm vi ảnh hưởng**: Tất cả các màn hình gọi API `/api/v1/issues` khi không có từ khóa tìm kiếm (Màn hình danh sách công việc khi lọc theo trạng thái/loại, màn hình chi tiết Epic, màn hình chi tiết Task xem Subtask).
- **Cách kiểm tra sau sửa**: Truy cập `/spaces/5/issues/7` (Epic) và các Task có Subtask -> Không còn lỗi 500, danh sách công việc con hiển thị đúng.

---

### BUG-03: Nhấp nháy và lỗi hiển thị khi chuyển trạng thái hoặc xóa mà không có Loading State tối ưu
- **Phân loại**: UI/UX
- **Mức độ**: **Minor**
- **Môi trường**: Web Next.js
- **Tiền điều kiện**: Đang ở trang chi tiết công việc.
- **Các bước tái hiện**:
  1. Bấm vào nút chuyển trạng thái (ví dụ "IN_PROGRESS" hoặc "DONE").
  2. Bấm liên tục 2 lần hoặc quan sát giao diện trong lúc API đang xử lý.
- **Kết quả mong đợi**: Nút bấm bị vô hiệu hóa (disabled), hiển thị spinner loading, tránh người dùng double click gây lỗi xung đột phiên bản (Version Conflict 409).
- **Kết quả thực tế**: Mặc dù có biến `actionLoading`, nhưng các nút trạng thái khác trong hàng không bị disable đồng bộ, người dùng click nhanh có thể kích hoạt 2 request cùng lúc.
- **Nguyên nhân kỹ thuật**: Trạng thái `actionLoading` chưa được áp dụng thuộc tính `disabled={actionLoading}` lên toàn bộ các nút action phụ cận.
- **Hướng xử lý**: Thêm `disabled={actionLoading}` và hiệu ứng spinner mờ trên toàn bộ thanh nút trạng thái.
- **Phạm vi ảnh hưởng**: Khối chuyển trạng thái trong trang `[issueId]/page.tsx`.
- **Cách kiểm tra sau sửa**: Click nút trạng thái -> Quan sát tất cả các nút lân cận đều bị vô hiệu hóa trong lúc request đang gửi đi.

---

## 4. ĐỀ XUẤT CẢI TIẾN UX (UX ENHANCEMENT PROPOSALS)

*(Tách biệt hoàn toàn khỏi danh sách lỗi)*

1. **UX-01: Tự động điền Parent Issue khi bấm "+ Thêm Subtask" từ trang chi tiết**:
   - *Hiện trạng*: Khi bấm "+ Thêm Subtask" từ trang chi tiết Task, người dùng bị điều hướng sang trang `/issues/new`. Tại đây, người dùng phải tự chọn lại Type = "SUBTASK", sau đó lại phải tìm kiếm và chọn lại tên Task cha trong dropdown.
   - *Đề xuất*: Khi bấm "+ Thêm Subtask" từ Task A, URL chuyển thành `/issues/new?type=SUBTASK&parentId=A`. Trang tạo tự động chọn sẵn Type = SUBTASK và khóa cố định Parent Issue là Task A.
2. **UX-02: Cải tiến Modal Xác nhận Xóa Công việc**:
   - *Hiện trạng*: Modal xóa hiện tại chỉ có tiêu đề "Xác nhận xóa công việc" và câu cảnh báo chung chung: "Bạn có chắc chắn muốn xóa công việc này? Hành động này không thể hoàn tác."
   - *Đề xuất*: Hiển thị rõ mã Issue Key và Tên tóm tắt cần xóa trong modal (Ví dụ: *"Bạn có chắc chắn muốn xóa [POTEST-3] Task 01 - Cấu hình kiểm tra..."*), kèm theo số lượng Subtask liên quan để người dùng tránh xóa nhầm.
3. **UX-03: Bổ sung Breadcrumb điều hướng phân cấp (Hierarchy Breadcrumb)**:
   - *Hiện trạng*: Breadcrumb chỉ hiển thị `Danh sách công việc / KEY`.
   - *Đề xuất*: Đối với Subtask, hiển thị: `Danh sách công việc / [POTEST-3] Task cha / [POTEST-4] Subtask`. Đối với Story thuộc Epic, hiển thị thẻ màu đại diện cho Epic ngay cạnh tiêu đề để dễ dàng nhận biết ngữ cảnh.
4. **UX-04: Hỗ trợ Inline Editing cho tiêu đề và mô tả**:
   - *Hiện trạng*: Người dùng phải chuyển trang hoặc mở form phức tạp.
   - *Đề xuất*: Theo chuẩn trải nghiệm của Jira/Linear, cho phép người dùng click trực tiếp vào Tiêu đề (Summary) hoặc Mô tả (Description) để gõ và ấn Enter (hoặc click ra ngoài) để tự động lưu qua API `PATCH`.

---

## 5. KẾ HOẠCH SỬA LỖI & PHÂN BỔ ƯU TIÊN

### 5.1. Thứ tự ưu tiên & Danh sách Module/File dự kiến thay đổi

| Ưu tiên | Mã lỗi | Module / File dự kiến thay đổi | Lý do ưu tiên | Rủi ro & Tác động |
| :---: | :---: | :--- | :--- | :--- |
| **P0** | **BUG-02** | `src/main/java/com/example/demo/respository/IssueRepository.java` | Lỗi 500 làm sập API truy vấn danh sách issue khi không có từ khóa `q`. Chặn luồng xem chi tiết của Epic và Task. | Rủi ro thấp. Chỉ cần ép kiểu `CAST(:q AS string)` hoặc tách hàm truy vấn chuẩn trong JPA. |
| **P0** | **BUG-01** | `uam-frontend/src/app/(workspace)/spaces/[spaceId]/issues/[issueId]/page.tsx`<br>`uam-frontend/src/components/issues/EditIssueModal.tsx` *(tạo mới)* | Tính năng Update là 1 trong 4 trụ cột bắt buộc của CRUD Issue, hiện tại hoàn toàn thiếu trên UI. | Cần đảm bảo validation form đồng bộ với Backend và xử lý Optimistic Locking (`version`). |
| **P1** | **UX-01** | `uam-frontend/src/app/(workspace)/spaces/[spaceId]/issues/[issueId]/page.tsx`<br>`uam-frontend/src/app/(workspace)/spaces/[spaceId]/issues/new/page.tsx` | Nâng cao trải nghiệm tạo Subtask nhanh từ công việc cha theo chuẩn Jira. | Rủi ro rất thấp (chỉ truyền query param qua URL). |
| **P2** | **BUG-03** & **UX-02** | `uam-frontend/src/app/(workspace)/spaces/[spaceId]/issues/[issueId]/page.tsx` | Ngăn ngừa lỗi xung đột bấm đúp và cải thiện an toàn dữ liệu khi xóa. | Rủi ro thấp (chỉ tinh chỉnh component frontend). |

### 5.2. Cách kiểm tra hồi quy sau khi sửa (Regression Testing Plan)
1. **Kiểm tra truy vấn (BUG-02)**:
   - Gọi API `GET /api/v1/issues?spaceId=5` (không có param `q`). Kết quả phải trả về HTTP 200 kèm danh sách công việc.
   - Mở màn hình Epic `/spaces/5/issues/7` trên trình duyệt: Không còn lỗi 500, hiển thị đầy đủ danh sách các Story con.
2. **Kiểm tra chỉnh sửa Issue (BUG-01)**:
   - Mở Task -> Bấm "Chỉnh sửa" -> Đổi Summary thành "Task đã cập nhật" -> Bấm Lưu -> Giao diện cập nhật ngay lập tức.
   - F5 tải lại trang -> Dữ liệu mới vẫn được duy trì.
   - Thử nghiệm gửi request với version cũ để kiểm tra thông báo xung đột phiên bản `409 VERSION_CONFLICT`.
3. **Kiểm tra toàn bộ ma trận CRUD**:
   - Chạy lại toàn bộ bộ kịch bản kiểm thử tự động Playwright (Suite 1 & Suite 2) từ bước đăng nhập, tạo space, tạo issue, chuyển trạng thái, đến xóa issue.

---

## 6. NHỮNG ĐIỂM CẦN PRODUCT OWNER (PO) XÁC NHẬN

1. **Phân quyền chỉnh sửa Issue**:
   - Hiện tại theo BR-02/AC-04: Người tạo (Reporter), Người được giao (Assignee) và Quản trị viên (Admin/Manager) có quyền cập nhật Issue.
   - *Cần xác nhận*: Thành viên thường (Member) trong Space nhưng không phải là Assignee/Reporter có được phép cập nhật `Priority` hoặc `Description` của Issue không, hay chỉ được bình luận?
2. **Quy định chuyển đổi loại công việc (Issue Type Conversion)**:
   - Hiện tại hệ thống không cho phép đổi Loại công việc (ví dụ đổi từ Task sang Story hoặc sang Bug) sau khi đã tạo.
   - *Cần xác nhận*: Trong phiên bản v0.1 này, chúng ta có giữ nguyên thiết kế cố định Issue Type sau khi tạo hay cần bổ sung tính năng Convert Issue Type?
3. **Quy tắc xóa Issue khi có dữ liệu liên kết Sprint**:
   - Hiện tại hệ thống chặn xóa Issue nếu Issue đang nằm trong một Sprint đang chạy (`ACTIVE`) hoặc đã đóng (`CLOSED`).
   - *Cần xác nhận*: Quy tắc này có áp dụng cho cả Subtask của Issue đó không? (Đề xuất: Nếu Task cha bị chặn xóa thì toàn bộ Subtask của nó cũng không được xóa riêng lẻ khi Sprint đang Active).
4. **Cơ chế phân cấp quan hệ Issue (Story - Epic)**:
   - Một Story có thể được gỡ khỏi Epic (chuyển về không gắn Epic) hay không?
   - *Đề xuất*: Cho phép gán hoặc gỡ Epic trong form Chỉnh sửa Issue thông qua tùy chọn "Không gắn Epic".
