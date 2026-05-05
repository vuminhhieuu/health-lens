package com.healthlens.api.security;

import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Story 1.6 — AC #3 verification for {@link JwtAuthenticationFilter}.
 * Confirms accounts in PENDING_DELETION / DELETED states are blocked with
 * HTTP 403 even when they present a valid JWT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - account status enforcement (Story 1.6 AC #3)")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private AccountStatusCache accountStatusCache;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil, redisTemplate, accountStatusCache, false);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AC #3: PENDING_DELETION account is blocked with 403 and cannot authenticate")
    void blocksPendingDeletionAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractJti(token)).thenReturn("jti-1");
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(jwtUtil.extractSubject(token)).thenReturn(userId.toString());
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("ROLE_USER");
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(accountStatusCache.getStatus(userId)).thenReturn(Optional.of(AccountStatus.PENDING_DELETION));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("Tài khoản đang chờ xóa");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("AC #3: DELETED account is also blocked with 403")
    void blocksDeletedAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractJti(token)).thenReturn("jti-2");
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(jwtUtil.extractSubject(token)).thenReturn(userId.toString());
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("ROLE_USER");
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(accountStatusCache.getStatus(userId)).thenReturn(Optional.of(AccountStatus.DELETED));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("Tài khoản đã bị xóa");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Active account passes through and authentication is set")
    void passesActiveAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractJti(token)).thenReturn("jti-3");
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(jwtUtil.extractSubject(token)).thenReturn(userId.toString());
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("ROLE_USER");
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(accountStatusCache.getStatus(userId)).thenReturn(Optional.of(AccountStatus.ACTIVE));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(userId.toString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Public deletion-cancellation endpoint short-circuits the filter (no JWT inspection)")
    void skipsCancellationEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/users/deletion-requests/cancel");
        request.setRequestURI("/api/v1/users/deletion-requests/cancel");
        request.addHeader("Authorization", "Bearer should.be.ignored");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(anyString());
        verify(accountStatusCache, never()).getStatus(any(UUID.class));
    }
}
