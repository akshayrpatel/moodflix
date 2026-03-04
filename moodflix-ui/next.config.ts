import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  reactCompiler: true,
  async rewrites() {
    return [
      {
        source: "/api/search/batch",
        destination: "http://localhost:8001/api/search/batch",
      },
      {
        source: "/api/search",
        destination: "http://localhost:8001/api/search",
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "image.tmdb.org",
        port: "",
        pathname: "/t/p/**",
      },
    ],
  },
};

export default nextConfig;
