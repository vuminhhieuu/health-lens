import { readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

describe("SettingsDirectContactCard", () => {
  const componentPath = join(
    process.cwd(),
    "src/app/(dashboard)/settings/_components/SettingsDirectContactCard.tsx",
  );

  it("renders phone and email actions from supportContact", () => {
    const source = readFileSync(componentPath, "utf8");

    expect(source).toContain("supportContact.phoneHref");
    expect(source).toContain("supportContact.phoneDisplay");
    expect(source).toContain("supportContact.email");
    expect(source).toContain("PUBLIC_SUPPORT_HREF");
    expect(source).toContain("rounded-full");
  });
});
