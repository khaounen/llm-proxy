package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.domain.RouteConfig;
import com.almersoul.llmproxy.service.ProxyMetricService;
import com.almersoul.llmproxy.service.ProxyRoutingService;
import com.almersoul.llmproxy.service.RouteConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmProxyController {

    private static final Set<String> RESERVED_PREFIXES = Set.of("/api/admin", "/api/auth", "/api/me", "/actuator", "/h2-console", "/error");

    private final RouteConfigService routeConfigService;
    private final ProxyRoutingService proxyRoutingService;
    private final ProxyMetricService proxyMetricService;

    public LlmProxyController(
            RouteConfigService routeConfigService,
            ProxyRoutingService proxyRoutingService,
            ProxyMetricService proxyMetricService) {
        this.routeConfigService = routeConfigService;
        this.proxyRoutingService = proxyRoutingService;
        this.proxyMetricService = proxyMetricService;
    }

    @RequestMapping(path = {"/", "/{*path}"}, method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE,
            org.springframework.web.bind.annotation.RequestMethod.HEAD
    })
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, Principal principal) {
        String incomingPath = request.getRequestURI();
        if (isReservedPath(incomingPath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        RouteConfig routeConfig = routeConfigService.findBestActiveRoute(incomingPath).orElse(null);
        if (routeConfig == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("No route configured for path: " + incomingPath).getBytes());
        }

        ProxyRoutingService.ProxyExchangeResult result = proxyRoutingService.forward(routeConfig, request);
        ResponseEntity<byte[]> proxiedResponse = ResponseEntity.status(result.response().getStatusCode())
                .headers(result.response().getHeaders())
                .header("X-Proxy-Route", routeConfig.getName())
                .body(result.response().getBody());

        URI targetUri = result.targetUri();
        if (targetUri != null) {
            proxyMetricService.recordAsync(
                    principal == null ? "unknown" : principal.getName(),
                    routeConfig.getName(),
                    incomingPath,
                    targetUri,
                    request.getMethod(),
                    proxiedResponse.getStatusCode().value(),
                    result.latencyMs(),
                    result.errorMessage(),
                    result.promptPreview());
        }

        if (HttpMethod.HEAD.matches(request.getMethod())) {
            return ResponseEntity.status(proxiedResponse.getStatusCode())
                    .headers(proxiedResponse.getHeaders())
                    .build();
        }
        return proxiedResponse;
    }

    private boolean isReservedPath(String path) {
        return RESERVED_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
