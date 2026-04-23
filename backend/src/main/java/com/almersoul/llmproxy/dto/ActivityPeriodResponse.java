package com.almersoul.llmproxy.dto;

import java.time.Instant;
import java.util.List;

public record ActivityPeriodResponse(
        Instant periodStart,
        Instant periodEnd,
        long totalRequests,
        long uniqueUsers,
        long totalSessions,
        long errorRequests,
        double errorRate,
        double avgLatencyMs,
        List<CountByLabelResponse> topConsumers,
        List<CountByLabelResponse> topRoutes) {}
