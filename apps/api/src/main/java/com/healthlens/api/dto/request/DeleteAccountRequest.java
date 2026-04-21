package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to delete user account with password confirmation.
 */
public record DeleteAccountRequest(
        @NotBlank(message = "Mat khau khong duoc de trong")
        String password
) {
}
