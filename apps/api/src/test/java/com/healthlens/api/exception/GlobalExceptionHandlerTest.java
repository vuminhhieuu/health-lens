package com.healthlens.api.exception;

import com.healthlens.api.security.LoginRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(mock(LoginRateLimiter.class));
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("DELETE", "/api/v1/users/deletion-requests/cancel");

    @Test
    @DisplayName("Cancel deletion invalid or expired token maps to 401 with stable error code")
    void deletionCancellationToken_mapsToUnauthorized() {
        var problem = handler.handleDeletionCancellationToken(
                new DeletionCancellationTokenException("Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực."),
                request);

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getProperties()).containsEntry(
                "errorCode",
                ApiErrorCode.DELETION_CANCEL_TOKEN_INVALID.value());
    }

    @Test
    @DisplayName("Cancel deletion replay maps to 409 conflict")
    void deletionCancellationConflict_mapsToConflict() {
        var problem = handler.handleDeletionCancellationConflict(
                new DeletionCancellationConflictException("Yêu cầu xóa này không thể hủy được"),
                request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("errorCode", ApiErrorCode.INVALID_STATE.value());
    }

    @Test
    @DisplayName("Cancel deletion forbidden account state maps to 403")
    void deletionCancellationForbidden_mapsToForbidden() {
        var problem = handler.handleDeletionCancellationForbidden(
                new DeletionCancellationForbiddenException("Không thể hủy yêu cầu xóa"),
                request);

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getProperties()).containsEntry("errorCode", ApiErrorCode.FORBIDDEN.value());
    }
}
