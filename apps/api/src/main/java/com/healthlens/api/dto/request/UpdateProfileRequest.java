package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for updating an existing user profile.
 *
 * Validation rules:
 * - displayName: Required, max 100 characters (trimmed by ProfileService before storage)
 * - notes: Optional, max 500 characters. Empty strings converted to null by service.
 * - birthDate: Optional, must be in the past
 * - gender: Optional, normalized to lower case by service
 *
 * Note: String length validation counts Java chars, not grapheme clusters.
 * This is safe for Vietnamese text but may over-count emoji and complex Unicode.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate birthDate,

        @Size(max = 10, message = "Giới tính quá dài")
        String gender,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String notes
) {
}