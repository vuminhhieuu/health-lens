package com.healthlens.api.config;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.constants.SecurityConstants;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher(SecurityConfig::requiresCsrfProtection)
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(ApiRoutes.ACTUATOR_HEALTH, ApiRoutes.ACTUATOR_HEALTH_PATTERN).permitAll()
                    .requestMatchers(ApiRoutes.AUTH_PATTERN).permitAll()
                    .requestMatchers(ApiRoutes.DEV_PATTERN).permitAll()
                    .requestMatchers(ApiRoutes.USERS_DELETION_PATTERN).permitAll()
                    .requestMatchers(ApiRoutes.INVITATIONS_ACCEPT).permitAll()
                    .requestMatchers(ApiRoutes.HEALTH_RECORD_INVITATIONS_ACCEPT).permitAll()
                    // Avoid masking app errors as 401 due to forwarding to /error.
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(ApiRoutes.SWAGGER_UI_PATTERN, ApiRoutes.SWAGGER_HTML, ApiRoutes.API_DOCS_PATTERN).permitAll()
                    .requestMatchers(ApiRoutes.ADMIN_AUTH_LOGIN).permitAll()
                    .requestMatchers(ApiRoutes.ADMIN_AUTH_TOTP_SETUP, ApiRoutes.ADMIN_AUTH_TOTP_VERIFY).authenticated()
                    .requestMatchers(ApiRoutes.ADMIN_PATTERN).hasAuthority(UserRole.ROLE_ADMIN.name())
                    .anyRequest().authenticated()
            )
            // Return 401 (not 403) for missing/invalid auth so frontend can trigger refresh flow.
            .exceptionHandling((ExceptionHandlingConfigurer<HttpSecurity> ex) ->
                    ex.authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase())
            ))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setHeaderName(SecurityConstants.XSRF_HEADER);
        return repository;
    }

    private static boolean requiresCsrfProtection(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return ApiRoutes.AUTH_LOGIN.equals(path)
                || ApiRoutes.AUTH_LOGOUT.equals(path)
                || (ApiRoutes.AUTH_REFRESH.equals(path) && hasRefreshTokenCookie(request));
    }

    private static boolean hasRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies != null && Arrays.stream(cookies)
                .anyMatch(cookie -> SecurityConstants.REFRESH_TOKEN_COOKIE.equals(cookie.getName()));
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken == null) {
                csrfToken = (CsrfToken) request.getAttribute("_csrf");
            }
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
