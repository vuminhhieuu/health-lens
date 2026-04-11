import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import RegisterPage from "./page";

vi.mock("axios", () => ({
  default: {
    post: vi.fn().mockResolvedValue({ status: 201 }),
  },
}));

describe("RegisterPage", () => {
  it("hien thi loi validation khi mat khau yeu", async () => {
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.type(screen.getByLabelText("Email"), "user@example.com");
    await user.type(screen.getByLabelText("Mật khẩu"), "weak");
    await user.type(screen.getByLabelText("Xác nhận mật khẩu"), "weak");
    await user.click(screen.getByRole("button", { name: /đăng ký/i }));

    await waitFor(() => {
      expect(screen.getByText("Mật khẩu tối thiểu 8 ký tự")).toBeInTheDocument();
    });
  });
});
