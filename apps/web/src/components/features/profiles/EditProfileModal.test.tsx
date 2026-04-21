import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { EditProfileModal } from "./EditProfileModal";

describe("EditProfileModal", () => {
  it("hien thi loi khi ten hien thi de trong", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(
      <EditProfileModal
        isOpen
        profile={{
          id: "7f4fd0a2-f741-4f78-8b31-1fb1f10265f8",
          displayName: "Me",
          birthDate: "1970-02-03",
          gender: "female",
          notes: "Tieu duong",
        }}
        onClose={vi.fn()}
        onSubmit={onSubmit}
        isLoading={false}
      />,
    );

    const displayNameInput = screen.getByPlaceholderText("Ví dụ: Mẹ, Bố...");
    await user.clear(displayNameInput);
    await user.type(displayNameInput, "   ");
    await user.click(screen.getByRole("button", { name: /Lưu thay đổi/i }));

    await waitFor(() => {
      expect(screen.getByText("Tên không được để trống")).toBeInTheDocument();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
