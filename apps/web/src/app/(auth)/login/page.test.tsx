import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import LoginPage from "./page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams("pendingDeletion=1"),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector: (state: { setAuth: () => void; setConsentState: () => void }) => unknown) =>
    selector({
      setAuth: vi.fn(),
      setConsentState: vi.fn(),
    }),
}));

describe("LoginPage", () => {
  it("hien thi trang thai chan dang nhap khi tai khoan dang cho xoa", async () => {
    render(<LoginPage />);

    expect(screen.getAllByText("Tài khoản đang chờ xóa").length).toBeGreaterThan(0);
    expect(screen.getByText("Đăng nhập không khả dụng")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Yêu cầu xóa tài khoản của bạn đã được ghi nhận. Theo Nghị định 13/2023/NĐ-CP, hệ thống đang trong quá trình xóa dữ liệu vĩnh viễn (tối đa 72 giờ). Trong thời gian này, bạn không thể đăng nhập.",
      ),
    ).toBeInTheDocument();

    expect(screen.getByRole("link", { name: "Mở email để hủy yêu cầu xóa" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Quay lại trang chủ" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Gửi lại" })).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Tài khoản đang chờ xóa/i }),
    ).toBeDisabled();
  });
});
