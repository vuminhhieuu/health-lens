package com.healthlens.api.support;

import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import jakarta.servlet.FilterChain;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Pass-through stubs for security filters required by {@link com.healthlens.api.config.SecurityConfig}
 * in {@code @WebMvcTest} slices.
 */
public final class SecurityFilterTestSupport {

    private SecurityFilterTestSupport() {
    }

    public static void stubPassthroughFilters(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserActivityRecordingFilter userActivityRecordingFilter) throws Exception {
        Answer<Void> passthrough = invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        };
        doAnswer(passthrough).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(passthrough).when(userActivityRecordingFilter).doFilter(any(), any(), any());
    }
}
