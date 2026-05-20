/**
 * sessionStorage can throw when disabled, in private mode, or over quota.
 * Callers should treat failures as "unavailable" rather than crashing the flow.
 */
export function readSessionStorage(key: string): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    return window.sessionStorage.getItem(key);
  } catch {
    return null;
  }
}

export function removeSessionStorage(key: string): void {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.sessionStorage.removeItem(key);
  } catch {
    // ignore
  }
}

export function writeSessionStorage(key: string, value: string): boolean {
  if (typeof window === "undefined") {
    return false;
  }

  try {
    window.sessionStorage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}
