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
});
