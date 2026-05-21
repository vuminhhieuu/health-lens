import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  FORGOT_PASSWORD_COOLDOWN_MS,
  forgotPasswordCooldownSeconds,
  getForgotPasswordCooldownRemainingMs,
  markForgotPasswordRequestSent,
} from "@/lib/auth/forgotPasswordCooldown";

describe("forgotPasswordCooldown", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("returns remaining cooldown within 60s window", () => {
    markForgotPasswordRequestSent(1_000);
    vi.setSystemTime(31_000);

    expect(getForgotPasswordCooldownRemainingMs()).toBe(30_000);
    expect(forgotPasswordCooldownSeconds(30_000)).toBe(30);
  });

  it("returns zero after cooldown expires", () => {
    markForgotPasswordRequestSent(0);
    vi.setSystemTime(FORGOT_PASSWORD_COOLDOWN_MS + 1);

    expect(getForgotPasswordCooldownRemainingMs()).toBe(0);
  });
});
