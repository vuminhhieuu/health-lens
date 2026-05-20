package com.healthlens.api.correlation;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;

public final class CorrelationContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TRACE_ID = "traceId";

    private static final ThreadLocal<Values> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static Values current() {
        return CURRENT.get();
    }

    public static String getCorrelationId() {
        Values values = current();
        return values == null ? null : values.correlationId();
    }

    public static String getRequestId() {
        Values values = current();
        return values == null ? null : values.requestId();
    }

    public static String getTraceId() {
        Values values = current();
        return values == null ? null : values.traceId();
    }

    public static Values ensure(String correlationId, String requestId, String traceId) {
        Values values = new Values(
                normalizeOrGenerate(correlationId),
                normalizeOrGenerate(requestId),
                normalizeOrGenerate(traceId)
        );
        set(values);
        return values;
    }

    public static Values ensureForJob(String correlationId) {
        return ensure(correlationId, null, null);
    }

    public static void set(Values values) {
        Values safeValues = Objects.requireNonNull(values, "values");
        CURRENT.set(safeValues);
        MDC.put(MDC_CORRELATION_ID, safeValues.correlationId());
        MDC.put(MDC_REQUEST_ID, safeValues.requestId());
        MDC.put(MDC_TRACE_ID, safeValues.traceId());
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_TRACE_ID);
    }

    public static String normalizeOrGenerate(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = value.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    public record Values(String correlationId, String requestId, String traceId) {
    }
}
