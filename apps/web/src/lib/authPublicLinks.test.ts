import { describe, expect, it } from "vitest";

import {
  AUTH_PUBLIC_FOOTER_LINKS,
  AUTH_PUBLIC_HELP_HREF,
  authPublicFooterHrefs,
} from "@/lib/authPublicLinks";

describe("authPublicLinks", () => {
  it("points help to public marketing route, not dashboard", () => {
    expect(AUTH_PUBLIC_HELP_HREF).toBe("/help");
    expect(authPublicFooterHrefs()).toEqual(["/privacy", "/terms", "/help"]);
  });

  it("uses Vietnamese labels aligned with login footer", () => {
    expect(AUTH_PUBLIC_FOOTER_LINKS.map((link) => link.label)).toEqual([
      "Quy định bảo mật",
      "Điều khoản sử dụng",
      "Trợ giúp",
    ]);
  });
});
