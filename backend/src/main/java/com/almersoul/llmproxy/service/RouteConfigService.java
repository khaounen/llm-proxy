package com.almersoul.llmproxy.service;

import com.almersoul.llmproxy.domain.OutboundAuthType;
import com.almersoul.llmproxy.domain.RouteConfig;
import com.almersoul.llmproxy.dto.RouteConfigRequest;
import com.almersoul.llmproxy.repository.RouteConfigRepository;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RouteConfigService {

    private final RouteConfigRepository repository;

    public RouteConfigService(RouteConfigRepository repository) {
        this.repository = repository;
    }

    public List<RouteConfig> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RouteConfig::getIncomingPrefix))
                .toList();
    }

    public Optional<RouteConfig> findBestActiveRoute(String incomingPath) {
        String normalizedPath = normalizePath(incomingPath);
        return repository.findByActiveTrue().stream()
                .sorted((a, b) -> Integer.compare(
                        b.getIncomingPrefix().length(),
                        a.getIncomingPrefix().length()))
                .filter(route -> matchesPrefix(normalizedPath, route.getIncomingPrefix()))
                .findFirst();
    }

    @Transactional
    public RouteConfig create(RouteConfigRequest request) {
        RouteConfig routeConfig = new RouteConfig();
        applyRequest(routeConfig, request);
        return repository.save(routeConfig);
    }

    @Transactional
    public RouteConfig update(Long id, RouteConfigRequest request) {
        RouteConfig routeConfig = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Route config not found"));
        applyRequest(routeConfig, request);
        return repository.save(routeConfig);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Route config not found");
        }
        repository.deleteById(id);
    }

    private void applyRequest(RouteConfig routeConfig, RouteConfigRequest request) {
        routeConfig.setName(request.getName().trim());
        routeConfig.setIncomingPrefix(normalizePath(request.getIncomingPrefix()));
        routeConfig.setTargetBaseUrl(normalizeTargetBaseUrl(request.getTargetBaseUrl()));
        OutboundAuthType outboundAuthType = request.getOutboundAuthType() == null
                ? OutboundAuthType.NONE
                : request.getOutboundAuthType();
        routeConfig.setOutboundAuthType(outboundAuthType);

        if (outboundAuthType == OutboundAuthType.BEARER) {
            if (!StringUtils.hasText(request.getOutboundBearerToken())) {
                throw new IllegalArgumentException("Outbound bearer token is required when auth type is BEARER");
            }
            routeConfig.setOutboundBearerToken(request.getOutboundBearerToken().trim());
        } else {
            routeConfig.setOutboundBearerToken(null);
        }

        routeConfig.setActive(request.isActive());
    }

    private static String normalizePath(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Incoming prefix is mandatory");
        }
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeTargetBaseUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Target base URL is mandatory");
        }
        String normalized = value.trim();
        URI.create(normalized);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
