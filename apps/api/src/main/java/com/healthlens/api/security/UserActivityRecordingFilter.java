package com.healthlens.api.security;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.service.UserActivityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.UUID;

/**
 * Records product analytics activity after the request completes successfully.
 * Must not run before business handlers (e.g. consent) — see story 8.2.
 */
@Component
public class UserActivityRecordingFilter extends OncePerRequestFilter {

    private final UserActivityService userActivityService;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    public UserActivityRecordingFilter(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            recordActivityIfApplicable(request, response);
        }
    }

    private void recordActivityIfApplicable(HttpServletRequest request, HttpServletResponse response) {
        if (response.getStatus() >= 400) {
            return;
        }

        String requestUri = urlPathHelper.getRequestUri(request);
        if (!shouldRecordForPath(request, requestUri)) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userIdString)) {
            return;
        }

        boolean isProductUser = authentication.getAuthorities().stream()
                .anyMatch(granted -> UserRole.ROLE_USER.name().equals(granted.getAuthority()));
        if (!isProductUser) {
            return;
        }

        try {
            userActivityService.recordAuthIfAbsent(UUID.fromString(userIdString));
        } catch (IllegalArgumentException ignored) {
            // Invalid subject — ignore.
        }
    }

    private boolean shouldRecordForPath(HttpServletRequest request, String requestUri) {
        if (requestUri == null || !requestUri.startsWith(ApiRoutes.API_V1 + "/")) {
            return false;
        }
        if (!isRecordableMethod(request.getMethod())) {
            return false;
        }
        if (requestUri.startsWith(ApiRoutes.ADMIN_BASE + "/")) {
            return false;
        }
        if (requestUri.startsWith(ApiRoutes.AUTH_BASE + "/")) {
            return false;
        }
        if (requestUri.startsWith(ApiRoutes.USERS_DELETION_BASE)) {
            return false;
        }
        // Consent must complete without side-effect writes in the request path.
        if (requestUri.startsWith(ApiRoutes.USERS_ME_CONSENT)) {
            return false;
        }
        return true;
    }

    private static boolean isRecordableMethod(String method) {
        return "GET".equalsIgnoreCase(method)
                || "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
