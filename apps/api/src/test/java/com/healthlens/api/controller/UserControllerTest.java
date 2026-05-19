package com.healthlens.api.controller;

import com.healthlens.api.dto.response.UserResponse;
import com.healthlens.api.exception.DeletionCancellationTokenException;
import com.healthlens.api.service.DataDeletionService;
import com.healthlens.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final DataDeletionService dataDeletionService = mock(DataDeletionService.class);
    private final UserController controller = new UserController(userService, dataDeletionService);

    @Test
    @DisplayName("PUT /users/me/avatar delegates authenticated multipart upload and wraps UserResponse")
    void uploadAvatar_DelegatesToUserService() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] { 1, 2, 3 });
        UserResponse response = userResponse(userId, "https://storage.local/avatar.png");
        when(userService.uploadAvatar(userId, file)).thenReturn(response);

        Map<String, Object> body = controller
                .uploadAvatar(new TestingAuthenticationToken(userId.toString(), null), file)
                .getBody();

        verify(userService).uploadAvatar(userId, file);
        assertThat(body).isNotNull();
        assertThat(body.get("data")).isEqualTo(response);
    }

    @Test
    @DisplayName("DELETE /users/me/avatar delegates authenticated removal and returns null avatarUrl")
    void removeAvatar_DelegatesToUserService() {
        UUID userId = UUID.randomUUID();
        UserResponse response = userResponse(userId, null);
        when(userService.removeAvatar(userId)).thenReturn(response);

        Map<String, Object> body = controller
                .removeAvatar(new TestingAuthenticationToken(userId.toString(), null))
                .getBody();

        verify(userService).removeAvatar(userId);
        assertThat(body).isNotNull();
        assertThat(body.get("data")).isEqualTo(response);
    }

    @Test
    @DisplayName("DELETE /users/deletion-requests/cancel delegates missing token to cancellation semantics")
    void cancelDeletion_MissingTokenDelegatesToService() {
        when(dataDeletionService.cancelDeletionRequest(null))
                .thenThrow(new DeletionCancellationTokenException(
                        "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực."));

        assertThatThrownBy(() -> controller.cancelDeletion(null))
                .isInstanceOf(DeletionCancellationTokenException.class);
        verify(dataDeletionService).cancelDeletionRequest(null);
    }

    private UserResponse userResponse(UUID userId, String avatarUrl) {
        return new UserResponse(
                userId,
                "mai@example.com",
                "Nguyen Mai",
                LocalDate.of(1990, 1, 1),
                "female",
                true,
                false,
                avatarUrl);
    }
}
