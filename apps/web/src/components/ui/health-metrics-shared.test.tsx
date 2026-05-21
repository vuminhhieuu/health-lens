import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { HealthMetricsGrid } from "./HealthMetricsGrid";
import { ReferenceRangeIndicator } from "./ReferenceRangeIndicator";

describe("shared health metric UI", () => {
  it("shows the shared empty state when there are no metrics", () => {
    render(<HealthMetricsGrid metrics={[]} onSelectMetric={vi.fn()} />);

    expect(screen.getByText("Chưa có chỉ số nào")).toBeInTheDocument();
    expect(screen.getByText("Hệ thống chưa trích xuất được chỉ số nào từ kết quả này.")).toBeInTheDocument();
  });

  it("selects a metric from the shared grid with click and keyboard", async () => {
    const user = userEvent.setup();
    const onSelectMetric = vi.fn();

    render(
      <HealthMetricsGrid
        metrics={[
          {
            name: "glucose",
            displayNameVi: "Glucose",
            value: "5.4",
            unit: "mmol/L",
            status: "normal",
            referenceRange: { min: 3.9, max: 5.6 },
          },
        ]}
        onSelectMetric={onSelectMetric}
      />
    );

    await user.click(screen.getByRole("button", { name: /glucose/i }));
    await user.keyboard("{Enter}");

    expect(onSelectMetric).toHaveBeenCalledTimes(2);
    expect(onSelectMetric).toHaveBeenNthCalledWith(1, 0);
    expect(onSelectMetric).toHaveBeenNthCalledWith(2, 0);
  });

  it("announces loading reference details and renders explanation text", () => {
    render(
      <ReferenceRangeIndicator
        unit="mmol/L"
        referenceRange={{ min: 3.9, max: 5.6, attentionMin: 3.5, attentionMax: 6.2 }}
        referenceRangeSource="system"
        rangeContext={{ gender: "female", ageRange: "18-40" }}
        critical
        explanation="Giải thích theo ngữ cảnh."
        isExplanationLoading={false}
        skeletonLines={2}
      />
    );

    expect(screen.getByText("Ngưỡng tham chiếu:")).toBeInTheDocument();
    expect(screen.getByText("Nguồn ngưỡng:")).toBeInTheDocument();
    expect(screen.getByText("Ngữ cảnh ngưỡng:")).toBeInTheDocument();
    expect(screen.getByText("Giải thích theo ngữ cảnh.")).toBeInTheDocument();
  });

  it("uses a polite live region while explanation skeleton is visible", () => {
    render(
      <ReferenceRangeIndicator
        unit="mmol/L"
        isExplanationLoading
        skeletonLines={2}
      />
    );

    expect(screen.getByText("Không có dữ liệu tham chiếu")).toBeInTheDocument();
    expect(screen.getByTestId("reference-range-skeleton")).toBeInTheDocument();
    expect(screen.getByTestId("reference-range-skeleton").parentElement).toHaveAttribute("aria-live", "polite");
  });
});
