import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import DashboardLayout from "./layout";

vi.mock("next/navigation", () => ({
  usePathname: () => "/home",
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
}));

vi.mock("@/hooks/useAuthBootstrap", () => ({
  useAuthBootstrap: () => false,
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector: (state: unknown) => unknown) =>
    selector({
      isAuthenticated: true,
      user: { email: "user@example.com", fullName: "Nguyen Van A" },
      clearAuth: vi.fn(),
    }),
}));

vi.mock("@/lib/api/apiClient", () => ({
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: { data: { fullName: "Nguyen Van A" } } }),
    post: vi.fn().mockResolvedValue({}),
  },
}));

describe("DashboardLayout", () => {
  it("icon dau hoi tren header dieu huong den trang thac mac", () => {
    render(
      <DashboardLayout>
        <div>Dashboard content</div>
      </DashboardLayout>,
    );

    expect(screen.getByRole("link", { name: "Mở trang thắc mắc" })).toHaveAttribute(
      "href",
      "/questions",
    );
  });
});
