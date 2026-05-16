import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import HelpPage from "./page";

describe("HelpPage", () => {
  it("hien thi cac nhom huong dan chinh va disclaimer y te", () => {
    render(<HelpPage />);

    expect(
      screen.getByRole("heading", { name: "Trang trợ giúp" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Tải kết quả khám")).toBeInTheDocument();
    expect(screen.getByText("Xem kết quả và giải thích chỉ số")).toBeInTheDocument();
    expect(screen.getByText("Quản lý hồ sơ")).toBeInTheDocument();
    expect(screen.getByText("Chia sẻ cho người thân")).toBeInTheDocument();
    expect(screen.getByText("Tải PDF kết quả")).toBeInTheDocument();
    expect(screen.getByText("Quyền riêng tư dữ liệu")).toBeInTheDocument();
    expect(
      screen.getByText(/không thay thế tư vấn y tế chuyên môn/i),
    ).toBeInTheDocument();
  });

  it("co lien ket den cac route dashboard lien quan", () => {
    render(<HelpPage />);

    expect(screen.getByRole("link", { name: /tải kết quả mới/i })).toHaveAttribute(
      "href",
      "/health-records",
    );
    expect(screen.getByRole("link", { name: /xem hồ sơ gia đình/i })).toHaveAttribute(
      "href",
      "/profiles",
    );
    expect(screen.getByRole("link", { name: /cập nhật hồ sơ cá nhân/i })).toHaveAttribute(
      "href",
      "/settings/profile",
    );
    expect(screen.getByRole("link", { name: /quản lý xóa dữ liệu/i })).toHaveAttribute(
      "href",
      "/settings/delete-account",
    );
  });
});
