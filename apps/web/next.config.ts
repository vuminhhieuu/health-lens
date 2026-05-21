import { readFileSync } from "node:fs";
import { join } from "node:path";

import type { NextConfig } from "next";

const packageVersion = JSON.parse(
  readFileSync(join(__dirname, "package.json"), "utf8"),
) as { version?: string };

const nextConfig: NextConfig = {
  output: "standalone",
  env: {
    NEXT_PUBLIC_APP_VERSION:
      process.env.NEXT_PUBLIC_APP_VERSION?.trim() || packageVersion.version || "0.0.0",
  },
};

export default nextConfig;
