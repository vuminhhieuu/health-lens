import { describe, expect, it } from "vitest";

import { APP_DISPLAY_NAME, getAppBuildLabel, getAppVersionLabel } from "./appVersion";

describe("appVersion", () => {
  it("exposes app display name", () => {
    expect(APP_DISPLAY_NAME).toBe("HealthLens");
  });

  it("returns version from env when set", () => {
    const previous = process.env.NEXT_PUBLIC_APP_VERSION;
    process.env.NEXT_PUBLIC_APP_VERSION = "2.3.4";
    expect(getAppVersionLabel()).toBe("2.3.4");
    if (previous === undefined) {
      delete process.env.NEXT_PUBLIC_APP_VERSION;
    } else {
      process.env.NEXT_PUBLIC_APP_VERSION = previous;
    }
  });

  it("returns build id when set", () => {
    const previous = process.env.NEXT_PUBLIC_BUILD_ID;
    process.env.NEXT_PUBLIC_BUILD_ID = "abc123";
    expect(getAppBuildLabel()).toBe("abc123");
    if (previous === undefined) {
      delete process.env.NEXT_PUBLIC_BUILD_ID;
    } else {
      process.env.NEXT_PUBLIC_BUILD_ID = previous;
    }
  });

  it("returns null build when env unset", () => {
    const previous = process.env.NEXT_PUBLIC_BUILD_ID;
    delete process.env.NEXT_PUBLIC_BUILD_ID;
    expect(getAppBuildLabel()).toBeNull();
    if (previous !== undefined) {
      process.env.NEXT_PUBLIC_BUILD_ID = previous;
    }
  });
});
