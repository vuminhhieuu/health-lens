import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import CancelDeletionClient from "./CancelDeletionClient";

const cancelDeletionMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams(""),
}));

vi.mock("../../hooks/useAccountDeletion", () => ({
  useAccountDeletion: () => ({
    cancelDeletion: cancelDeletionMock,
  }),
}));

describe("CancelDeletionClient", () => {
  it("hien thi loi lien ket khong hop le khi khong co token (theo Stitch)", async () => {
    const user = userEvent.setup();
    render(<CancelDeletionClient />);

    await user.click(screen.getByRole("button", { name: /Hủy yêu cầu xóa \(khôi phục tài khoản\)/i }));

    expect(
      screen.getByRole("heading", { name: "Liên kết không hợp lệ hoặc đã hết hạn" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Liên kết có thể đã hết hạn hoặc không đúng định dạng/i),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("example@healthlens.vn")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Gửi lại email hủy yêu cầu/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Về trang đăng nhập/i })).toBeInTheDocument();
    expect(screen.queryByText(/DELETE_TOKEN_INVALID/i)).not.toBeInTheDocument();
    expect(cancelDeletionMock).not.toHaveBeenCalled();
  });
});
