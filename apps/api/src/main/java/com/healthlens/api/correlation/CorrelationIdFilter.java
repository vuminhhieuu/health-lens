package com.healthlens.api.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CorrelationContext.Values values = CorrelationContext.ensure(
                request.getHeader(CorrelationContext.CORRELATION_ID_HEADER),
                request.getHeader(CorrelationContext.REQUEST_ID_HEADER),
                request.getHeader(CorrelationContext.TRACE_ID_HEADER)
        );
        response.setHeader(CorrelationContext.CORRELATION_ID_HEADER, values.correlationId());
        response.setHeader(CorrelationContext.REQUEST_ID_HEADER, values.requestId());
        response.setHeader(CorrelationContext.TRACE_ID_HEADER, values.traceId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationContext.clear();
        }
    }
}
