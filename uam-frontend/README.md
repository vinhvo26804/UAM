# UAM frontend

Nền tảng Next.js App Router, React, TypeScript, Tailwind CSS và ESLint dùng npm.
Frontend nằm ở `UAM/uam-frontend`, ngang cấp thư mục source backend `UAM/src`;
backend hiện ở gốc repository, không được di chuyển hoặc chỉnh sửa.

Các trang hiện chỉ hiển thị tiêu đề và **Chưa triển khai**. Chưa có đăng nhập,
lưu/refresh token, form, bảng dữ liệu hoặc kiểm tra phiên phía frontend.
Layout workspace/admin hiện truy cập được để xem khung trang; quyền truy cập API
và nghiệp vụ vẫn do Spring Boot kiểm tra.

## Chạy trên máy

Đã kiểm tra với Node.js **24.21.0**, npm **11.19.0**. Project yêu cầu Node.js
**22.18+** để chạy cả kiểm thử TypeScript trực tiếp bằng Node. Next.js **16.3.8**
stable được xác minh qua npm registry lúc thiết lập; phiên bản phụ thuộc được
khóa trong `package-lock.json`. [Yêu cầu Next.js chính thức](https://nextjs.org/docs/app/getting-started/installation).

Từ thư mục `UAM`, chạy PowerShell:

```powershell
cd uam-frontend
npm ci
# Chỉ sao chép nếu chưa có file, tránh ghi đè cấu hình riêng:
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
npm run dev
```

Mở `http://localhost:3000`. Mọi lệnh npm bên dưới cũng chạy trong `uam-frontend`.

```powershell
npm run lint
npm run test:http
npm run build
npm start
```

`npm start` chạy bản production sau khi build; dừng dev server trước nếu cùng
dùng cổng 3000. Build và các trang nền không cần backend hoặc tải font từ mạng.

## Môi trường và HTTP

`.env.local` đã được tạo cho máy hiện tại và được Git bỏ qua.
`.env.example` được giữ trong source với cấu hình:

```dotenv
BACKEND_URL=http://localhost:8089
```

Cổng **8089** đã xác minh trong `../src/main/resources/application.yaml`.
`BACKEND_URL` là địa chỉ gốc backend, không thêm `/backend`. Next.js đọc biến này
ở phía server; không dùng tiền tố `NEXT_PUBLIC_`. Đổi biến thì khởi động lại dev;
với production, build lại rồi khởi động lại. Không đưa JWT secret hoặc cấu hình
database vào frontend.

`next.config.ts` rewrite `/backend/:path*` → `${BACKEND_URL}/:path*`.
Ví dụ trình duyệt gọi `/backend/me` thì Next.js chuyển tiếp tới backend `/me`.

`src/lib/http.ts` **chỉ dùng từ trình duyệt**, chẳng hạn trong event handler của
Client Component hoặc service được component đó gọi; không gọi từ Server
Component, Server Action hoặc lúc build. Ví dụ chỉ đọc với endpoint đã xác minh:

```ts
import { http, HttpError } from "@/lib/http";

// Trong event handler phía trình duyệt:
try {
  const response = await http("/v3/api-docs", { method: "GET" });
  console.log(response.status, response.data);
} catch (error) {
  if (error instanceof HttpError) console.error(error.status, error.data);
  else throw error; // lỗi mạng hoặc hủy request
}
```

Helper hỗ trợ method, JSON `body`, `headers`, `signal` và các Fetch options.
Kết quả có `status`, `headers`, `data` (JSON/text/`null` khi rỗng, bao gồm 204).
JSON sai định dạng được giữ dưới dạng text. HTTP ngoài 2xx ném `HttpError`
giữ status/body/headers; lỗi mạng và abort được truyền nguyên gốc. Generic `T`
chỉ mô tả JSON dự kiến, không validate dữ liệu runtime. Helper không đọc storage,
không tự gắn token, không tự refresh; caller có thể truyền Authorization sau khi
luồng xác thực được triển khai.

## Source

| Thư mục | Trách nhiệm |
| --- | --- |
| `src/app/` | Route, metadata, layout, CSS toàn cục; Server Component mặc định |
| `src/components/` | Header, sidebar, component trang chưa triển khai |
| `src/services/` | Hàm gọi API theo chức năng khi có nơi sử dụng; hiện có hướng dẫn |
| `src/lib/` | Fetch helper dùng chung |
| `src/types/` | DTO request/response thực tế khi cần sử dụng; hiện có hướng dẫn |
| `public/` | Tài nguyên tĩnh, hiện có icon UAM |
| `tests/` | Kiểm thử hợp đồng HTTP bằng mock Fetch, không gọi backend thật |
| `docs/` | Hợp đồng backend đã đọc và kết quả kiểm tra |

Routes: `/`, `/login`, `/register`, `/verify-otp`, `/forgot-password`,
`/dashboard`, `/profile`, `/security`, `/admin/users`, `/admin/users/[id]`.
`(auth)` và `(workspace)` chỉ nhóm layout, không xuất hiện trên URL.
Alias `@/*` trỏ đến `src/*`. Chưa tạo service/type nghiệp vụ chưa được sử dụng.

## Backend và phần còn thiếu

Xem [hợp đồng backend](docs/backend-contract.md) để biết chính xác endpoint,
DTO và các điểm cần đối chiếu trước khi triển khai nghiệp vụ:

- JWT stateless qua `Authorization: Bearer <accessToken>`; chưa chọn cách lưu
  token ở FE, không có hợp đồng cookie hiện tại.
- Chưa có controller quên/đặt lại mật khẩu hoạt động; không tự đặt endpoint.
- Cần xác nhận kênh nhận OTP và thống nhất lỗi; verify OTP có nhánh trả HTTP 200
  kèm text thất bại, không thể xem HTTP 2xx là thành công nghiệp vụ.
- Cần kiểm tra response thực tế khi backend hoạt động, nhất là các trường ngày,
  giá trị null và các tên `userName`/`username`, `dob`/`doB`, `lockUntill`.

Khi backend đang chạy, kiểm tra chỉ đọc qua proxy:

```powershell
Invoke-RestMethod http://localhost:3000/backend/v3/api-docs
```

Xem [kết quả kiểm tra và danh sách file](docs/verification.md). `AGENTS.md` và
`CLAUDE.md` được Next.js tự sinh để hướng dẫn agent đọc tài liệu đúng phiên bản.
