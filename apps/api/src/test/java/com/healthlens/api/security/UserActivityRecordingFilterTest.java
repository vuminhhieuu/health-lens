package com.healthlens.api.security;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.service.UserActivityService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityRecordingFilter (Story 8.2)")
class UserActivityRecordingFilterTest {

    @Mock private UserActivityService userActivityService;
    @Mock private FilterChain filterChain;

    private UserActivityRecordingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserActivityRecordingFilter(userActivityService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("records AUTH for ROLE_USER on successful product API call")
    void recordsAuthForRoleUser() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UserRole.ROLE_USER.name());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setRequestURI("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        verify(userActivityService).recordAuthIfAbsent(eq(userId));
    }

    @Test
    @DisplayName("skips admin routes")
    void skipsAdminRoutes() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UserRole.ROLE_ADMIN.name());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiRoutes.ADMIN_ANALYTICS_ACTIVITY);
        request.setRequestURI(ApiRoutes.ADMIN_ANALYTICS_ACTIVITY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        verify(userActivityService, never()).recordAuthIfAbsent(any());
    }

    @Test
    @DisplayName("skips consent endpoint")
    void skipsConsentEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UserRole.ROLE_USER.name());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", ApiRoutes.USERS_ME_CONSENT);
        request.setRequestURI(ApiRoutes.USERS_ME_CONSENT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        verify(userActivityService, never()).recordAuthIfAbsent(any());
    }

    @Test
    @DisplayName("skips failed responses")
    void skipsFailedResponses() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UserRole.ROLE_USER.name());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setRequestURI("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        filter.doFilter(request, response, filterChain);

        verify(userActivityService, never()).recordAuthIfAbsent(any());
    }

    @Test
    @DisplayName("skips non ROLE_USER even when authenticated")
    void skipsNonProductUserRole() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UserRole.ROLE_ADMIN.name());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles");
        request.setRequestURI("/api/v1/profiles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        verify(userActivityService, never()).recordAuthIfAbsent(any());
    }

    private static void authenticate(UUID userId, String role) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
