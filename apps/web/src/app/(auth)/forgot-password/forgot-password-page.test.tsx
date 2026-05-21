import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import ForgotPasswordPage from "@/app/(auth)/forgot-password/page";
import { markForgotPasswordRequestSent } from "@/lib/auth/forgotPasswordCooldown";
import { messageCatalog } from "@/lib/i18n/messages";

const postMock = vi.fn();

vi.mock("@/lib/api/apiClient", () => ({
  apiClient: {
    post: (...args: unknown[]) => postMock(...args),
  },
}));

vi.mock("@/lib/notify", () => ({
  notify: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

function axiosError(status?: number, payload?: Record<string, unknown>) {
  return {
    response: status
      ? {
          status,
          data: payload,
        }
      : undefined,
  };
}

describe("ForgotPasswordPage", () => {
  beforeEach(() => {
    postMock.mockReset();
    sessionStorage.clear();
  });

  it("blocks a second submit within 60s on the client", () => {
    markForgotPasswordRequestSent(Date.now());

    render(<ForgotPasswordPage />);

    expect(screen.getByRole("button", { name: /Thử lại sau \d+ giây/ })).toBeDisabled();
    expect(screen.getByRole("alert")).toHaveTextContent(
      messageCatalog.auth.forgotPasswordCooldown(60),
    );
    expect(postMock).not.toHaveBeenCalled();
  });

  it("shows enumeration-safe success with live region", async () => {
    postMock.mockResolvedValueOnce({ data: { data: {} } });
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText("Email"), "user@example.com");
    await user.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toBeInTheDocument();
    });
    expect(screen.getByText(messageCatalog.auth.forgotPasswordSent)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Quay lại đăng nhập" })).toHaveAttribute("href", "/login");
  });

  it("maps network failure to network copy", async () => {
    postMock.mockRejectedValueOnce(new Error("network"));
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText("Email"), "user@example.com");
    await user.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(messageCatalog.auth.networkError);
    });
  });

  it("maps server failure to generic retry copy", async () => {
    postMock.mockRejectedValueOnce(axiosError(500));
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText("Email"), "user@example.com");
    await user.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(messageCatalog.auth.genericRetry);
    });
  });

  it("maps 429 to rate-limit copy with retry minutes", async () => {
    postMock.mockRejectedValueOnce(
      axiosError(429, { retryAfterSeconds: 3600, errorCode: "RATE_LIMITED" }),
    );
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText("Email"), "user@example.com");
    await user.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        messageCatalog.auth.forgotPasswordRateLimited(60),
      );
    });
  });

  it("preserves email when resending from success state", async () => {
    postMock.mockResolvedValue({ data: { data: {} } });
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText("Email");
    await user.type(emailInput, "keep@example.com");
    await user.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "gửi lại yêu cầu" })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "gửi lại yêu cầu" }));

    await waitFor(() => {
      const emailAfterResend = screen.getByLabelText("Email");
      expect(emailAfterResend).toHaveValue("keep@example.com");
      expect(emailAfterResend).toHaveFocus();
    });
  });
});
