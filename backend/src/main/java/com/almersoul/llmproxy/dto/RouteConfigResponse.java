package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.OutboundAuthType;
import com.almersoul.llmproxy.domain.RouteConfig;
import java.time.Instant;

public record RouteConfigResponse(
        Long id,
        String name,
        String incomingPrefix,
        String targetBaseUrl,
        OutboundAuthType outboundAuthType,
        String outboundBearerToken,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static RouteConfigResponse from(RouteConfig routeConfig) {
        return new RouteConfigResponse(
                routeConfig.getId(),
                routeConfig.getName(),
                routeConfig.getIncomingPrefix(),
                routeConfig.getTargetBaseUrl(),
                routeConfig.getOutboundAuthType(),
                routeConfig.getOutboundBearerToken(),
                routeConfig.isActive(),
                routeConfig.getCreatedAt(),
                routeConfig.getUpdatedAt());
    }
}
