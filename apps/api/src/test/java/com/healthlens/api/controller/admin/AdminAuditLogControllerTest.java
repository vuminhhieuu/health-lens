package com.healthlens.api.controller.admin;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.admin.AuditLogEntryDto;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.admin.AdminAuditLogService;
import com.healthlens.api.support.SecurityFilterTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuditLogController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuditLogService adminAuditLogService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserActivityRecordingFilter userActivityRecordingFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
    }

    @Test
    @DisplayName("GET /admin/audit-logs — admin được phép, trả page JSON")
    void list_returnsPageForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        whenPageReturned(new AuditLogPageDto(
                List.of(new AuditLogEntryDto(
                        id,
                        "admin@healthlens.vn",
                        "LOGIN",
                        "AUTH",
                        null,
                        "Phiên đăng nhập",
                        "Đăng nhập thành công",
                        "SUCCESS",
                        null,
                        "{\"email\":\"admin@healthlens.vn\"}",
                        null,
                        "corr-1",
                        null,
                        null,
                        "127.0.0.1",
                        Instant.parse("2026-05-10T10:00:00Z")
                )),
                1,
                0,
                20
        ));

        mockMvc.perform(get(ApiRoutes.ADMIN_AUDIT_LOGS)
                        .param("page", "0")
                        .param("limit", "20")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorEmail").value("admin@healthlens.vn"))
                .andExpect(jsonPath("$.content[0].detailSummary").value("Đăng nhập thành công"));
    }

    @Test
    @DisplayName("GET /admin/audit-logs/export — UTF-8 BOM + header CSV (smoke AC #3/#7)")
    void export_writesUtf8BomBeforeCsvBody() throws Exception {
        doAnswer(invocation -> {
            var writer = invocation.getArgument(7, java.io.Writer.class);
            writer.write("id,actorEmail\n");
            writer.write("row-id,admin@healthlens.vn\n");
            return null;
        }).when(adminAuditLogService).writeCsv(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(),
                eq(10_000)
        );

        MvcResult result = mockMvc.perform(get(ApiRoutes.ADMIN_AUDIT_LOGS_EXPORT)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("system-audit-logs.csv")))
                .andExpect(contentTypeContainsUtf8Csv())
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThanOrEqualTo(3);
        assertThat(body[0]).isEqualTo((byte) 0xEF);
        assertThat(body[1]).isEqualTo((byte) 0xBB);
        assertThat(body[2]).isEqualTo((byte) 0xBF);
        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("actorEmail");
        verify(adminAuditLogService).writeCsv(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), eq(10_000));
    }

    @Test
    @DisplayName("GET /admin/audit-logs — không admin → 401")
    void list_requiresAdminRole() throws Exception {
        mockMvc.perform(get(ApiRoutes.ADMIN_AUDIT_LOGS))
                .andExpect(status().isUnauthorized());
    }

    private void whenPageReturned(AuditLogPageDto page) {
        try {
            org.mockito.Mockito.when(adminAuditLogService.query(
                    any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
            )).thenReturn(page);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentTypeContainsUtf8Csv() {
        return result -> assertThat(result.getResponse().getContentType())
                .contains("text/csv")
                .contains("UTF-8");
    }
}
