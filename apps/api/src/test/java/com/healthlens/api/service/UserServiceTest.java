package com.healthlens.api.service;

import com.healthlens.api.dto.request.UpdateUserRequest;
import com.healthlens.api.dto.response.UserResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Old Name");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setGender("male");
        testUser.setEmailVerified(true);
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getCurrentUser(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.fullName()).isEqualTo("Old Name");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.gender()).isEqualTo("male");
    }

    @Test
    void getCurrentUser_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại");
    }

    @Test
    void updateCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "New Name  ",
                LocalDate.of(1995, 5, 5),
                "other");

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(response.fullName()).isEqualTo("New Name"); // should be trimmed
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(response.gender()).isEqualTo("other");

        verify(userRepository).save(testUser);
    }

    @Test
    void updateCurrentUser_PartialUpdate_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        // Update only full name
        UpdateUserRequest request = new UpdateUserRequest("Just Name", null, null);

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(response.fullName()).isEqualTo("Just Name");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1)); // unchanged
        assertThat(response.gender()).isEqualTo("male"); // unchanged

        verify(userRepository).save(testUser);
    }

    @Test
    void updateCurrentUser_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest("Name", LocalDate.of(1995, 5, 5), "other");

        assertThatThrownBy(() -> userService.updateCurrentUser(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
