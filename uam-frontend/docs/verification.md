# Kiểm tra frontend

Kiểm tra tại máy hiện tại ngày 2026-10-05, Node.js 24.21.0, npm 11.19.0.

| Kiểm tra đã chạy | Kết quả |
| --- | --- |
| `npm run lint` | PASS, 0 lỗi, 0 cảnh báo |
| `npm run test:http` | PASS, 10/10 test; mock Fetch, không tác động dữ liệu backend |
| `npm run build` | PASS, compile TypeScript và production build khi backend không chạy |
| `npm run dev -- --hostname 127.0.0.1` | Khởi động thành công tại cổng 3000 |
| Chrome qua agent-browser | Trang chủ, login, dashboard hiển thị; click điều hướng từ trang chủ → login → dashboard thành công |
| Layout mobile 390 × 844 | Login và dashboard hiển thị đầy đủ, không tràn ngang; `scrollWidth = innerWidth = 390` |
| Console/browser | Không có lỗi JavaScript/hydration hoặc error overlay được ghi nhận |
| HTTP của 10 route | Tất cả 200, có `main-content` và thông báo “Chưa triển khai” |
| `.env.local` / `.env.example` | File local bị Git ignore; file mẫu không bị ignore |
| Rewrite trong production manifest | `/backend/:path*` → `http://localhost:8089/:path*` |

10 route đã thử: `/`, `/login`, `/register`, `/verify-otp`, `/forgot-password`,
`/dashboard`, `/profile`, `/security`, `/admin/users`, `/admin/users/check-route`.
`check-route` chỉ dùng kiểm tra dynamic route, không đại diện user tồn tại và
không phát sinh API request. Ảnh kiểm tra desktop/mobile lưu ở `artifacts/`
trên máy; thư mục này được Git ignore.

**Thiếu điều kiện:** GET trực tiếp `http://localhost:8089/v3/api-docs` thất bại
vì không kết nối được. Chưa kiểm chứng vòng request `/backend` tới Spring Boot
thật hoặc JSON trả về lúc runtime. Không tự khởi động backend/migration hay
gọi API ghi dữ liệu. Khi backend chạy, dùng lệnh GET proxy trong README.

**Ngoài phạm vi:** luồng đăng nhập, lưu/refresh token, OTP, phân quyền UI,
quản lý user và test nghiệp vụ backend. Không deploy, commit hoặc push.

## File được tạo

Tất cả nằm trong `uam-frontend/`; không sửa file backend có sẵn.

```text
.env.example
.env.local                   # máy hiện tại, Git ignore
.gitignore
AGENTS.md                    # Next.js tự sinh
CLAUDE.md                    # Next.js tự sinh
README.md
package.json
package-lock.json
next.config.ts
next-env.d.ts
tsconfig.json
eslint.config.mjs
postcss.config.mjs
public/icon.svg
src/app/layout.tsx
src/app/page.tsx
src/app/globals.css
src/app/(auth)/layout.tsx
src/app/(auth)/login/page.tsx
src/app/(auth)/register/page.tsx
src/app/(auth)/verify-otp/page.tsx
src/app/(auth)/forgot-password/page.tsx
src/app/(workspace)/layout.tsx
src/app/(workspace)/dashboard/page.tsx
src/app/(workspace)/profile/page.tsx
src/app/(workspace)/security/page.tsx
src/app/(workspace)/admin/users/page.tsx
src/app/(workspace)/admin/users/[id]/page.tsx
src/components/header.tsx
src/components/sidebar.tsx
src/components/placeholder-page.tsx
src/lib/http.ts
src/services/README.md
src/types/README.md
tests/http.test.mjs
docs/backend-contract.md
docs/verification.md
```

`node_modules/`, `.next/`, `artifacts/` là dữ liệu cài đặt/build/kiểm tra và
không được đưa vào source. Bốn file backend đã có thay đổi từ trước vẫn giữ
nguyên: `AuthController.java`, `RefreshToken.java`, `RefreshTokenRepository.java`,
`AuthService.java`.
