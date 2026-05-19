import { afterEach, describe, expect, it, vi } from "vitest";

import { readSessionStorage, writeSessionStorage } from "./sessionStorage";

describe("sessionStorage helpers", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.sessionStorage.clear();
  });

  it("reads and writes values when storage is available", () => {
    expect(writeSessionStorage("test-key", "1")).toBe(true);
    expect(readSessionStorage("test-key")).toBe("1");
  });

  it("returns null/false when storage throws", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("denied");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("denied");
    });

    expect(readSessionStorage("test-key")).toBeNull();
    expect(writeSessionStorage("test-key", "1")).toBe(false);
  });
});
