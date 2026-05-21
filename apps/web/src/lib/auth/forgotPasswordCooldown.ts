import {
  readSessionStorage,
  writeSessionStorage,
} from "@/lib/browser/sessionStorage";

export const FORGOT_PASSWORD_COOLDOWN_MS = 60_000;

const STORAGE_KEY = "forgot-password-last-request-at";

export function getForgotPasswordCooldownRemainingMs(): number {
  const raw = readSessionStorage(STORAGE_KEY);
  if (!raw) {
    return 0;
  }

  const lastRequestAt = Number(raw);
  if (!Number.isFinite(lastRequestAt)) {
    return 0;
  }

  return Math.max(0, FORGOT_PASSWORD_COOLDOWN_MS - (Date.now() - lastRequestAt));
}

export function markForgotPasswordRequestSent(at = Date.now()) {
  writeSessionStorage(STORAGE_KEY, String(at));
}

export function forgotPasswordCooldownSeconds(remainingMs: number) {
  return Math.max(1, Math.ceil(remainingMs / 1000));
}
