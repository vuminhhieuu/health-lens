package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FollowUpReminderRequest(
        @NotNull(message = "Ngày nhắc là bắt buộc")
        LocalDate reminderDate,

        @NotBlank(message = "Loại nhắc là bắt buộc")
        @Size(max = 50, message = "Loại nhắc tối đa 50 ký tự")
        String reminderType,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
}
