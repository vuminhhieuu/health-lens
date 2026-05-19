package com.healthlens.api.service;

import com.healthlens.api.dto.request.AdminLoginRequest;
import com.healthlens.api.dto.response.AdminLoginResponse;
import com.healthlens.api.dto.response.AdminTotpSetupResponse;
import com.healthlens.api.entity.AdminTotpSecret;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.AdminTotpSecretRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.AdminAuthRateLimiter;
import com.healthlens.api.util.JwtUtil;

import io.jsonwebtoken.Claims;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.warrenstrange.googleauth.GoogleAuthenticator;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminTotpSecretRepository totpSecretRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private TotpSecretCryptoService cryptoService;
    @Mock private AdminAuthRateLimiter rateLimiter;
    @Mock private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private GoogleAuthenticator googleAuth;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthService(
                userRepository, totpSecretRepository, passwordEncoder,
                jwtUtil, cryptoService, rateLimiter, redisTemplate, googleAuth, auditEventRecorder
        );
    }

    // ========== LOGIN TESTS ==========

    @Nested
    @DisplayName("Admin Login")
    class LoginTests {

        @Test
        @DisplayName("login password dung + TOTP co verify → yeu cau TOTP code (AC #1)")
        void login_correctPassword_totpSetUp_noCode() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "AdminPass1", null);
            AdminTotpSecret totpSecret = createVerifiedTotpSecret(admin.getId());

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("AdminPass1", admin.getPasswordHash())).thenReturn(true);
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));
            when(jwtUtil.generateAdminAccessToken(eq(admin), eq(false))).thenReturn("temp-token");

            AdminLoginResponse response = adminAuthService.login(request);

            assertThat(response.totpRequired()).isTrue();
            assertThat(response.totpSetupRequired()).isFalse();
            assertThat(response.accessToken()).isEqualTo("temp-token");
            verify(rateLimiter).resetAttempts("admin@healthlens.vn");
        }

        @Test
        @DisplayName("login sai password throw BadCredentialsException (AC #2)")
        void login_wrongPassword() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "WrongPass", null);

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("WrongPass", admin.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Email hoặc mật khẩu không đúng.");

            verify(rateLimiter).recordFailure("admin@healthlens.vn");
        }

        @Test
        @DisplayName("login voi user khong phai admin throw BadCredentialsException")
        void login_notAdmin() {
            User regularUser = createRegularUser();
            AdminLoginRequest request = new AdminLoginRequest("user@healthlens.vn", "Pass1", null);

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("user@healthlens.vn")).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Email hoặc mật khẩu không đúng.");

            verify(rateLimiter).recordFailure("user@healthlens.vn");
        }

        @Test
        @DisplayName("login email khong ton tai throw BadCredentialsException")
        void login_emailNotFound() {
            AdminLoginRequest request = new AdminLoginRequest("unknown@healthlens.vn", "Pass1", null);

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("unknown@healthlens.vn")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Email hoặc mật khẩu không đúng.");
        }

        @Test
        @DisplayName("login admin chua setup TOTP → totpSetupRequired=true (AC #3)")
        void login_noTotpSetup() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "AdminPass1", null);

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("AdminPass1", admin.getPasswordHash())).thenReturn(true);
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.empty());
            when(jwtUtil.generateAdminAccessToken(eq(admin), eq(false))).thenReturn("temp-token");

            AdminLoginResponse response = adminAuthService.login(request);

            assertThat(response.totpSetupRequired()).isTrue();
            assertThat(response.totpRequired()).isFalse();
            assertThat(response.accessToken()).isEqualTo("temp-token");
            verify(rateLimiter).resetAttempts("admin@healthlens.vn");
        }



        @Test
        @DisplayName("login voi TOTP code sai throw BadCredentialsException (AC #2)")
        void login_wrongTotp() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "AdminPass1", "999999");
            AdminTotpSecret totpSecret = createVerifiedTotpSecret(admin.getId());

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("AdminPass1", admin.getPasswordHash())).thenReturn(true);
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));
            when(cryptoService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");
            when(googleAuth.authorize("JBSWY3DPEHPK3PXP", 999999)).thenReturn(false);

            // The TOTP code 999999 will be rejected by GoogleAuthenticator mock
            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Mã xác thực không hợp lệ hoặc đã hết hạn");

            verify(rateLimiter).recordFailure("admin@healthlens.vn");
        }

        @Test
        @DisplayName("login voi TOTP code khong phai so throw BadCredentialsException")
        void login_invalidTotpFormat() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "AdminPass1", "abcdef");
            AdminTotpSecret totpSecret = createVerifiedTotpSecret(admin.getId());

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("AdminPass1", admin.getPasswordHash())).thenReturn(true);
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));
            when(cryptoService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");

            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Mã xác thực không hợp lệ");
        }

        @Test
        @DisplayName("login admin co TOTP unverified → totpSetupRequired=true (AC #3)")
        void login_totpUnverified() {
            User admin = createAdminUser();
            AdminLoginRequest request = new AdminLoginRequest("admin@healthlens.vn", "AdminPass1", null);
            AdminTotpSecret unverifiedTotp = createUnverifiedTotpSecret(admin.getId());

            doNothing().when(rateLimiter).checkLocked(anyString());
            when(userRepository.findByEmailIgnoreCase("admin@healthlens.vn")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("AdminPass1", admin.getPasswordHash())).thenReturn(true);
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(unverifiedTotp));
            when(jwtUtil.generateAdminAccessToken(eq(admin), eq(false))).thenReturn("temp-token");

            AdminLoginResponse response = adminAuthService.login(request);

            assertThat(response.totpSetupRequired()).isTrue();
            assertThat(response.totpRequired()).isFalse();
        }
    }

    // ========== TOTP SETUP TESTS ==========

    @Nested
    @DisplayName("TOTP Setup")
    class TotpSetupTests {

        @Test
        @DisplayName("setupTotp tao secret va tra ve QR code URL (AC #3)")
        void setupTotp_success() {
            User admin = createAdminUser();
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.empty());
            when(cryptoService.encrypt(anyString())).thenReturn("encrypted-new-secret");
            
            GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder("JBSWY3DPEHPK3PXP").build();
            when(googleAuth.createCredentials()).thenReturn(key);

            AdminTotpSetupResponse response = adminAuthService.setupTotp(admin.getId());

            assertThat(response.secret()).isNotBlank();
            assertThat(response.qrCodeUrl()).contains("otpauth://totp/");
            assertThat(response.qrCodeUrl()).contains("issuer=HealthLens");
            verify(totpSecretRepository).save(any(AdminTotpSecret.class));
        }

        @Test
        @DisplayName("setupTotp cho user khong phai admin throw exception")
        void setupTotp_notAdmin() {
            User regularUser = createRegularUser();
            when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> adminAuthService.setupTotp(regularUser.getId()))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Không có quyền admin");
        }
    }

    // ========== TOTP VERIFY TESTS ==========

    @Nested
    @DisplayName("TOTP Verify")
    class TotpVerifyTests {

        @Test
        @DisplayName("verifyTotp voi code sai throw BadCredentialsException")
        void verifyTotp_wrongCode() {
            User admin = createAdminUser();
            AdminTotpSecret totpSecret = createUnverifiedTotpSecret(admin.getId());

            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            doNothing().when(rateLimiter).checkLocked(admin.getEmail());
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));
            when(cryptoService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");
            when(googleAuth.authorize(eq("JBSWY3DPEHPK3PXP"), any(Integer.class))).thenReturn(false);

            assertThatThrownBy(() -> adminAuthService.verifyTotp(admin.getId(), "999999"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Mã xác thực không hợp lệ hoặc đã hết hạn");
            
            verify(rateLimiter).recordFailure(admin.getEmail());
        }

        @Test
        @DisplayName("verifyTotp chua setup TOTP throw BadCredentialsException")
        void verifyTotp_noSetup() {
            User admin = createAdminUser();
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            doNothing().when(rateLimiter).checkLocked(admin.getEmail());
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminAuthService.verifyTotp(admin.getId(), "123456"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Chưa thiết lập TOTP");
            
            verify(rateLimiter).recordFailure(admin.getEmail());
        }

        @Test
        @DisplayName("verifyTotp code khong phai so throw BadCredentialsException")
        void verifyTotp_nonNumericCode() {
            User admin = createAdminUser();
            AdminTotpSecret totpSecret = createUnverifiedTotpSecret(admin.getId());

            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            doNothing().when(rateLimiter).checkLocked(admin.getEmail());
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));

            assertThatThrownBy(() -> adminAuthService.verifyTotp(admin.getId(), "abcdef"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Mã xác thực không hợp lệ");
            
            verify(rateLimiter).recordFailure(admin.getEmail());
        }

        @Test
        @DisplayName("verifyTotp thanh cong tra ve full admin token")
        void verifyTotp_success() {
            User admin = createAdminUser();
            AdminTotpSecret totpSecret = createUnverifiedTotpSecret(admin.getId());

            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            doNothing().when(rateLimiter).checkLocked(admin.getEmail());
            when(totpSecretRepository.findByUserId(admin.getId())).thenReturn(Optional.of(totpSecret));
            when(cryptoService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");
            when(googleAuth.authorize("JBSWY3DPEHPK3PXP", 123456)).thenReturn(true);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtUtil.generateAdminAccessToken(admin, true)).thenReturn("full-admin-token");

            AdminLoginResponse response = adminAuthService.verifyTotp(admin.getId(), "123456");

            assertThat(response.accessToken()).isEqualTo("full-admin-token");
            verify(rateLimiter).resetAttempts(admin.getEmail());
            verify(totpSecretRepository).save(totpSecret);
            verify(valueOperations).set(anyString(), eq("1"), any(java.time.Duration.class));
            assertThat(totpSecret.isVerified()).isTrue();
        }
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @DisplayName("logout blacklist admin access token theo thời hạn còn lại")
    void logout_blacklistAdminToken() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtUtil.extractJti("admin-token")).thenReturn("admin-jti");
        when(jwtUtil.extractClaims("admin-token")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 120_000));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adminAuthService.logout("admin-token");

        verify(valueOperations).set(
                eq("blacklist:token:admin-jti"),
                eq("1"),
                any(java.time.Duration.class)
        );
    }

    // ========== HELPERS ==========

    private User createAdminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@healthlens.vn");
        user.setFullName("Admin User");
        user.setPasswordHash("$2a$12$hashedAdminPassword");
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_ADMIN);
        return user;
    }

    private User createRegularUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");
        user.setFullName("Regular User");
        user.setPasswordHash("$2a$12$hashedPassword");
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        return user;
    }

    private AdminTotpSecret createVerifiedTotpSecret(UUID userId) {
        AdminTotpSecret secret = new AdminTotpSecret();
        secret.setUserId(userId);
        secret.setEncryptedSecret("encrypted-secret");
        secret.setVerified(true);
        secret.setCreatedAt(Instant.now());
        return secret;
    }

    private AdminTotpSecret createUnverifiedTotpSecret(UUID userId) {
        AdminTotpSecret secret = new AdminTotpSecret();
        secret.setUserId(userId);
        secret.setEncryptedSecret("encrypted-secret");
        secret.setVerified(false);
        secret.setCreatedAt(Instant.now());
        return secret;
    }
}
