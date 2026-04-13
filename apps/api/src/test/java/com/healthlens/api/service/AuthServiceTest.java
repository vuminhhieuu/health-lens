package com.healthlens.api.service;

import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.WeakPasswordException;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, passwordEncoder, emailService);
    }

    @Test
    @DisplayName("register thanh cong: hash password, save user, tao token, gui email")
    void register_success() {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "user@example.com", LocalDate.of(1999, 1, 1), "StrongPass1");
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        UUID result = authService.register(request);

        assertThat(result).isEqualTo(userId);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(userCaptor.getValue().getBirthDate()).isEqualTo(LocalDate.of(1999, 1, 1));
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
        verify(tokenRepository).save(any());
        verify(emailService).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("register that bai khi email trung")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "user@example.com", LocalDate.of(1999, 1, 1), "StrongPass1");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    @DisplayName("register that bai khi password yeu")
    void register_weakPassword() {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "user@example.com", LocalDate.of(1999, 1, 1), "weakpass");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(WeakPasswordException.class);

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }
}
