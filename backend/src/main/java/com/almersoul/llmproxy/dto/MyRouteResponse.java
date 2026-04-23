package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.RouteConfig;

public record MyRouteResponse(
        String name,
        String incomingPrefix) {

    public static MyRouteResponse from(RouteConfig routeConfig) {
        return new MyRouteResponse(routeConfig.getName(), routeConfig.getIncomingPrefix());
    }
}
