package com.almersoul.llmproxy.dto;

import java.time.Instant;

public record AdminDashboardResponse(
        Instant generatedAt,
        ActivityPeriodResponse daily,
        ActivityPeriodResponse monthly) {}
