import type { NextConfig } from "next";

const backendUrl = (process.env.BACKEND_URL || "http://localhost:8089").replace(
  /\/+$/,
  "",
);

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: `${backendUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
