import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import QuestionsPage from "./page";

describe("QuestionsPage", () => {
  it("hien thi FAQ toi thieu va disclaimer y te", () => {
    render(<QuestionsPage />);

    expect(
      screen.getByRole("heading", { name: "Thắc mắc thường gặp" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Nếu OCR không đọc được thì làm gì?")).toBeInTheDocument();
    expect(
      screen.getByText("Chỉ số bất thường có nghĩa là tôi đang bệnh nặng không?"),
    ).toBeInTheDocument();
    expect(screen.getByText("HealthLens có thay thế bác sĩ không?")).toBeInTheDocument();
    expect(
      screen.getByText("Người thân được xem những gì khi tôi chia sẻ?"),
    ).toBeInTheDocument();
    expect(screen.getByText("Tôi có thể thu hồi quyền chia sẻ không?")).toBeInTheDocument();
    expect(screen.getByText("Tôi có thể xóa dữ liệu sức khỏe không?")).toBeInTheDocument();
    expect(screen.getByText("Tại sao tôi không đăng nhập hoặc tải file được?")).toBeInTheDocument();
    expect(
      screen.getByText(/không thay thế tư vấn y tế chuyên môn/i),
    ).toBeInTheDocument();
  });
});
