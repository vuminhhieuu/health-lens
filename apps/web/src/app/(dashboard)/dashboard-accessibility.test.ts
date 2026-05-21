import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const webRoot = process.cwd();

function listTsxFiles(directory: string): string[] {
  const entries = readdirSync(directory, { withFileTypes: true });
  const files: string[] = [];

  for (const entry of entries) {
    const fullPath = join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...listTsxFiles(fullPath));
      continue;
    }
    if (entry.isFile() && entry.name.endsWith(".tsx") && !entry.name.includes(".test.")) {
      files.push(fullPath);
    }
  }

  return files;
}

const dashboardPages = listTsxFiles(join(webRoot, "src/app/(dashboard)")).filter(
  (path) => !path.endsWith("layout.tsx"),
);

const featureComponents = listTsxFiles(join(webRoot, "src/components/features")).filter(
  (path) =>
    path.includes("Modal") ||
    path.includes("Consent") ||
    path.includes("Upload") ||
    path.includes("HealthMetrics"),
);

describe("dashboard accessibility wiring", () => {
  it.each(dashboardPages)(
    "dashboard route has async accessibility semantics",
    (filePath) => {
      const source = readFileSync(filePath, "utf8");
      const hasAsyncUi =
        /isLoading|isError|saveError|deleteError|submitError|LoadingState|ErrorState/.test(source);

      if (!hasAsyncUi) {
        return;
      }

      expect(source).toMatch(
        /LoadingState|ErrorState|EmptyState|aria-live|role="alert"|role="status"|InlineFieldError/,
      );
    },
  );

  it.each(featureComponents)(
    "feature component links labels or announces errors",
    (filePath) => {
      const source = readFileSync(filePath, "utf8");

      if (source.includes("<label")) {
        expect(source).toMatch(/htmlFor=/);
      }

      const requiresErrorAnnouncement =
        source.includes("InlineFieldError") ||
        /errors\.\w+/.test(source) ||
        /submitError|setError\(|errorView|passwordError|avatarError/.test(source);

      if (requiresErrorAnnouncement) {
        expect(source).toMatch(/aria-live|role="alert"|InlineFieldError/);
      }
    },
  );
});
