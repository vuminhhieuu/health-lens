package com.healthlens.api.service;

import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("register luu user voi password da hash, email chưa verified, role ROLE_USER")
    void register_persistsUserWithExpectedState() {
        RegisterRequest request = new RegisterRequest("Integration User", "integration@example.com",
                LocalDate.of(2000, 2, 20), "StrongPass1");

        UUID userId = authService.register(request);

        User saved = userRepository.findById(userId).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("integration@example.com");
        assertThat(saved.getFullName()).isEqualTo("Integration User");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(2000, 2, 20));
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("StrongPass1");
        assertThat(passwordEncoder.matches("StrongPass1", saved.getPasswordHash())).isTrue();

        verify(emailService).sendVerificationEmail(any(User.class), anyString());
    }
}
