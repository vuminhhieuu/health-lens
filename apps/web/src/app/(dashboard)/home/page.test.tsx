import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import DashboardHomePage from "./page";

vi.mock("@/lib/api/apiClient", () => ({
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: { data: [] } }),
    post: vi.fn().mockResolvedValue({ data: { data: null } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}));

vi.mock("@tanstack/react-query", () => ({
  useMutation: () => ({
    mutate: vi.fn(),
    isPending: false,
  }),
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === "home-profiles") {
      return {
        data: [{ id: "profile-1", displayName: "Hồ sơ của tôi", isDefault: true }],
        isFetching: false,
      };
    }

    return { data: [], isFetching: false };
  },
  useQueryClient: () => ({
    invalidateQueries: vi.fn(),
    setQueryData: vi.fn(),
  }),
}));

describe("DashboardHomePage", () => {
  it("lien ket tro giup tren Home dieu huong den /help", () => {
    render(<DashboardHomePage />);

    expect(screen.getByRole("link", { name: /trợ giúp/i })).toHaveAttribute(
      "href",
      "/help",
    );
    expect(screen.getByRole("link", { name: /đọc hướng dẫn/i })).toHaveAttribute(
      "href",
      "/help",
    );
  });

  it("hien thi thao tac nhanh tu phuc vu thay cho tile chua kha dung", () => {
    render(<DashboardHomePage />);

    expect(screen.queryByText("Đặt lịch khám")).not.toBeInTheDocument();
    expect(screen.queryByText("Liên hệ bác sĩ")).not.toBeInTheDocument();

    expect(
      screen.getByRole("link", { name: /nhắc lịch tái khám/i }),
    ).toHaveAttribute("href", "/follow-up-reminders");
    expect(
      screen.getByRole("link", { name: /tóm tắt đi khám/i }),
    ).toHaveAttribute("href", "/visit-summary");
  });
});
