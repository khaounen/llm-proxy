package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.ProxyMetric;
import java.time.Instant;

public record ProxyMetricResponse(
        Long id,
        Long sessionId,
        String username,
        String routeName,
        String incomingPath,
        String targetUrl,
        String method,
        int statusCode,
        long latencyMs,
        String promptPreview,
        String errorMessage,
        Instant createdAt) {

    public static ProxyMetricResponse from(ProxyMetric metric) {
        return new ProxyMetricResponse(
                metric.getId(),
                metric.getSession() == null ? null : metric.getSession().getId(),
                metric.getUsername(),
                metric.getRouteName(),
                metric.getIncomingPath(),
                metric.getTargetUrl(),
                metric.getMethod(),
                metric.getStatusCode(),
                metric.getLatencyMs(),
                metric.getPromptPreview(),
                metric.getErrorMessage(),
                metric.getCreatedAt());
    }
}
