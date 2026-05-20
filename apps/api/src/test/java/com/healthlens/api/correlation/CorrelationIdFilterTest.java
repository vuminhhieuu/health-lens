package com.healthlens.api.correlation;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
        MDC.clear();
    }

    @Test
    void propagatesInboundCorrelationIdToContextMdcAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader(CorrelationContext.CORRELATION_ID_HEADER, "client-correlation-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) -> {
            assertThat(CorrelationContext.getCorrelationId()).isEqualTo("client-correlation-123");
            assertThat(MDC.get(CorrelationContext.MDC_CORRELATION_ID)).isEqualTo("client-correlation-123");
            assertThat(CorrelationContext.getRequestId()).isNotBlank();
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationContext.CORRELATION_ID_HEADER)).isEqualTo("client-correlation-123");
        assertThat(response.getHeader(CorrelationContext.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(CorrelationContext.getCorrelationId()).isNull();
        assertThat(MDC.get(CorrelationContext.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenRequestDoesNotProvideOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(CorrelationContext.getCorrelationId()).isNotBlank());

        assertThat(response.getHeader(CorrelationContext.CORRELATION_ID_HEADER)).isNotBlank();
        assertThat(response.getHeader(CorrelationContext.REQUEST_ID_HEADER)).isNotBlank();
    }
}
