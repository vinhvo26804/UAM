import Link from "next/link";

export default function HomePage() {
  return (
    <main id="main-content" className="mx-auto max-w-5xl px-6 py-12 sm:py-16">
      <section className="max-w-2xl">
        <div className="inline-flex items-center gap-2 rounded border border-[#85B8FF] bg-[#E9F2FF] px-2.5 py-0.5 text-xs font-semibold text-[#0C66E4]">
          <span className="h-1.5 w-1.5 rounded-full bg-[#0C66E4]" />
          Hệ thống Quản lý Tài khoản & Phân quyền Doanh nghiệp
        </div>

        <h1 className="mt-4 text-3xl sm:text-4xl font-bold tracking-tight text-[#172B4D] leading-tight">
          Cổng Quản lý Tài khoản Người dùng (UAM)
        </h1>

        <p className="mt-3 text-sm sm:text-base leading-relaxed text-[#626F86]">
          Không gian tự phục vụ hồ sơ cá nhân và bảng điều khiển quản trị tập trung, bảo vệ dữ liệu với xác thực JWT Stateless, kiểm soát phiên làm việc và phân quyền đa vai trò.
        </p>

        <div className="mt-6 flex flex-wrap gap-2.5">
          <Link
            href="/login"
            className="rounded bg-[#0C66E4] px-4 py-2 text-xs sm:text-sm font-semibold text-white hover:bg-[#0055CC] transition-colors"
          >
            Đăng nhập hệ thống
          </Link>
          <Link
            href="/register"
            className="rounded border border-[#DCDFE4] bg-white px-4 py-2 text-xs sm:text-sm font-semibold text-[#172B4D] hover:bg-[#F7F8F9] transition-colors"
          >
            Đăng ký tài khoản
          </Link>
          <Link
            href="/dashboard"
            className="rounded border border-[#85B8FF] bg-[#E9F2FF] px-4 py-2 text-xs sm:text-sm font-semibold text-[#0C66E4] hover:bg-[#CCE0FF] transition-colors"
          >
            Không gian làm việc &rarr;
          </Link>
        </div>

        <div className="mt-12 grid grid-cols-1 sm:grid-cols-3 gap-4 pt-8 border-t border-[#DCDFE4]">
          <div className="rounded border border-[#DCDFE4] bg-white p-3.5">
            <h2 className="text-xs font-bold text-[#172B4D] uppercase tracking-wider">Xác thực an toàn</h2>
            <p className="mt-1 text-xs text-[#626F86] leading-relaxed">
              Xác thực kép qua mã OTP email, cơ chế hàng đợi cấp mới Refresh Token tự động.
            </p>
          </div>
          <div className="rounded border border-[#DCDFE4] bg-white p-3.5">
            <h2 className="text-xs font-bold text-[#172B4D] uppercase tracking-wider">Hồ sơ cá nhân</h2>
            <p className="mt-1 text-xs text-[#626F86] leading-relaxed">
              Tự cập nhật họ tên, liên lạc, quy trình đổi địa chỉ email bảo mật với OTP.
            </p>
          </div>
          <div className="rounded border border-[#DCDFE4] bg-white p-3.5">
            <h2 className="text-xs font-bold text-[#172B4D] uppercase tracking-wider">Quản trị toàn diện</h2>
            <p className="mt-1 text-xs text-[#626F86] leading-relaxed">
              Tra cứu đa tiêu chí, phân quyền động, khóa và kích hoạt tài khoản tức thì.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
