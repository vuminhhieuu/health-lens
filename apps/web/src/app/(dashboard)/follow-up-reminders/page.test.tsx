import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import FollowUpRemindersPage from "./page";

const profiles = [
  { id: "profile-1", displayName: "Hồ sơ mặc định", isDefault: true },
  { id: "profile-2", displayName: "Hồ sơ của mẹ", isDefault: false },
];

let searchParams = new URLSearchParams("");
let mockedProfiles = profiles;
let mockedReminders: Array<{
  id: string;
  profileId: string;
  reminderDate: string;
  reminderType: string;
  note: string | null;
  emailSentAt: string | null;
  createdAt: string;
  updatedAt: string;
}> = [];

vi.mock("next/navigation", () => ({
  useSearchParams: () => searchParams,
}));

vi.mock("@/lib/api/apiClient", () => ({
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: { data: [] } }),
    post: vi.fn(async (_url: string, payload: { reminderDate: string; reminderType: string; note: string | null }) => {
      const reminder = {
        id: `reminder-${mockedReminders.length + 1}`,
        profileId: "profile-1",
        ...payload,
        emailSentAt: null,
        createdAt: "2026-05-16T00:00:00Z",
        updatedAt: "2026-05-16T00:00:00Z",
      };
      return { data: { data: reminder } };
    }),
    put: vi.fn(async (_url: string, payload: { reminderDate: string; reminderType: string; note: string | null }) => {
      const reminder = {
        ...mockedReminders[0],
        ...payload,
        updatedAt: "2026-05-17T00:00:00Z",
      };
      return { data: { data: reminder } };
    }),
    delete: vi.fn(async () => {
      return { data: { data: null } };
    }),
  },
}));

vi.mock("@tanstack/react-query", () => ({
  useMutation: (options: {
    mutationFn: (variables: unknown) => Promise<unknown>;
    onSuccess?: (data: unknown, variables: unknown) => void;
    onError?: (error: unknown) => void;
  }) => ({
    mutate: async (variables: unknown) => {
      try {
        const data = await options.mutationFn(variables);
        options.onSuccess?.(data, variables);
      } catch (error) {
        options.onError?.(error);
      }
    },
    isPending: false,
  }),
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === "follow-up-reminders") {
      return {
        data: mockedReminders,
        isLoading: false,
        isError: false,
      };
    }

    return {
      data: mockedProfiles,
      isLoading: false,
      isError: false,
    };
  },
  useQueryClient: () => ({
    setQueryData: (_queryKey: unknown, updater: unknown) => {
      if (typeof updater === "function") {
        mockedReminders = updater(mockedReminders);
      }
    },
  }),
}));

describe("FollowUpRemindersPage", () => {
  beforeEach(() => {
    searchParams = new URLSearchParams("");
    mockedProfiles = profiles;
    mockedReminders = [];
  });

  it("render title, breadcrumb va cac truong form", () => {
    render(<FollowUpRemindersPage />);

    expect(
      screen.getByRole("heading", { name: "Nhắc lịch tái khám" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Trang chủ")).toBeInTheDocument();
    expect(screen.getByText("Ngày nhắc")).toBeInTheDocument();
    expect(screen.getByLabelText(/loại nhắc/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/ghi chú/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /lưu nhắc lịch/i }),
    ).toBeInTheDocument();
  });

  it("chon ho so tu profileId query neu hop le", () => {
    searchParams = new URLSearchParams("profileId=profile-2");

    render(<FollowUpRemindersPage />);

    expect(screen.getByLabelText(/hồ sơ/i)).toHaveValue("profile-2");
    expect(screen.getByText(/nhắc lịch cho hồ sơ của mẹ/i)).toBeInTheDocument();
  });

  it("validate truong bat buoc va khong luu du lieu loi", async () => {
    const user = userEvent.setup();
    render(<FollowUpRemindersPage />);

    await user.click(screen.getByRole("button", { name: /lưu nhắc lịch/i }));

    expect(screen.getByText("Vui lòng chọn ngày nhắc.")).toBeInTheDocument();
    expect(mockedReminders).toHaveLength(0);
  });

  it("luu vao API va xoa reminder", async () => {
    const user = userEvent.setup();
    const { rerender } = render(<FollowUpRemindersPage />);

    await user.type(screen.getByLabelText(/ngày nhắc/i), "2026-06-20");
    await user.selectOptions(screen.getByLabelText(/loại nhắc/i), "Xét nghiệm lại");
    await user.type(screen.getByLabelText(/ghi chú/i), "Mang phiếu xét nghiệm cũ");
    await user.click(screen.getByRole("button", { name: /lưu nhắc lịch/i }));
    rerender(<FollowUpRemindersPage />);

    expect(screen.getByText("Đã lưu nhắc lịch cho hồ sơ này.")).toBeInTheDocument();
    expect(screen.getAllByText("Xét nghiệm lại")).toHaveLength(2);
    expect(screen.getByText("Mang phiếu xét nghiệm cũ")).toBeInTheDocument();
    expect(mockedReminders).toHaveLength(1);

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      await user.click(screen.getByRole("button", { name: /xóa nhắc lịch/i }));
      rerender(<FollowUpRemindersPage />);
    } finally {
      confirmSpy.mockRestore();
    }

    await waitFor(() => {
      expect(screen.queryByText("Mang phiếu xét nghiệm cũ")).not.toBeInTheDocument();
    });
    expect(mockedReminders).toHaveLength(0);
  });

  it("cho phep sua reminder da luu", async () => {
    const user = userEvent.setup();
    const { rerender } = render(<FollowUpRemindersPage />);

    await user.type(screen.getByLabelText(/ngày nhắc/i), "2026-06-20");
    await user.selectOptions(screen.getByLabelText(/loại nhắc/i), "Xét nghiệm lại");
    await user.type(screen.getByLabelText(/ghi chú/i), "Mang phiếu xét nghiệm cũ");
    await user.click(screen.getByRole("button", { name: /lưu nhắc lịch/i }));
    rerender(<FollowUpRemindersPage />);

    await user.click(screen.getByRole("button", { name: /sửa nhắc lịch/i }));
    expect(screen.getByRole("heading", { name: /sửa nhắc lịch/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/ngày nhắc/i)).toHaveValue("2026-06-20");

    await user.clear(screen.getByLabelText(/ngày nhắc/i));
    await user.type(screen.getByLabelText(/ngày nhắc/i), "2026-07-05");
    await user.selectOptions(screen.getByLabelText(/loại nhắc/i), "Theo dõi chỉ số");
    await user.clear(screen.getByLabelText(/ghi chú/i));
    await user.type(screen.getByLabelText(/ghi chú/i), "Kiểm tra lại HbA1c");
    await user.click(screen.getByRole("button", { name: /cập nhật nhắc lịch/i }));
    rerender(<FollowUpRemindersPage />);

    expect(screen.getByText("Đã cập nhật nhắc lịch cho hồ sơ này.")).toBeInTheDocument();
    expect(screen.getAllByText("Theo dõi chỉ số")).toHaveLength(2);
    expect(screen.getByText("Kiểm tra lại HbA1c")).toBeInTheDocument();
    expect(screen.queryByText("Mang phiếu xét nghiệm cũ")).not.toBeInTheDocument();
    expect(mockedReminders).toHaveLength(1);
  });

  it("hien thi empty state khi chua co ho so", () => {
    mockedProfiles = [];

    render(<FollowUpRemindersPage />);

    expect(screen.getByText(/bạn chưa có hồ sơ sức khỏe/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /quản lý hồ sơ/i })).toHaveAttribute(
      "href",
      "/profiles",
    );
  });
});
