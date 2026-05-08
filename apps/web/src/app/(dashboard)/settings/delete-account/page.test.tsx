import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import DeleteAccountPage from "./page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn(),
  }),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector: (state: { clearAuth: () => void }) => unknown) =>
    selector({ clearAuth: vi.fn() }),
}));

vi.mock("@/hooks/useAccountDeletion", () => ({
  useAccountDeletion: () => ({
    requestDeletion: vi.fn(),
    isRequesting: false,
    error: null,
    requestSuccess: false,
  }),
}));

describe("DeleteAccountPage", () => {
  it("hien thi giao dien yeu cau xoa tai khoan theo file tham chieu", () => {
    render(<DeleteAccountPage />);

    expect(screen.getByText("Yêu cầu xóa tài khoản")).toBeInTheDocument();
    expect(screen.getByText(/Chúng tôi rất tiếc khi thấy bạn rời đi/i)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Cảnh báo quan trọng" })).toBeInTheDocument();
    expect(screen.getByText("Thông tin định danh (PII)")).toBeInTheDocument();
    expect(screen.getByText("Hồ sơ bệnh án & Vitals")).toBeInTheDocument();
    expect(screen.getByText("Tệp đính kèm & Kết quả xét nghiệm")).toBeInTheDocument();
    expect(screen.getByText("Nhật ký đồng thuận dữ liệu")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Xác nhận danh tính" })).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Nhập mật khẩu của bạn")).toBeInTheDocument();
    expect(screen.getByRole("checkbox")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Gửi yêu cầu xóa/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Hủy bỏ" })).toBeInTheDocument();
  });

  it("chan submit neu chua tick xac nhan", async () => {
    const user = userEvent.setup();
    render(<DeleteAccountPage />);

    await user.type(screen.getByPlaceholderText("Nhập mật khẩu của bạn"), "secret");
    await user.click(screen.getByRole("button", { name: /Gửi yêu cầu xóa/i }));

    expect(
      screen.getByText("Vui lòng xác nhận bạn đã hiểu hậu quả của việc xóa tài khoản"),
    ).toBeInTheDocument();
  });
});
