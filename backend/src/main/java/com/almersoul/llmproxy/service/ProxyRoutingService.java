package com.almersoul.llmproxy.service;

import com.almersoul.llmproxy.domain.OutboundAuthType;
import com.almersoul.llmproxy.domain.RouteConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class ProxyRoutingService {

    private static final Set<String> HOP_HEADERS = Set.of(
            "host",
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "content-length");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ProxyRoutingService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public ProxyExchangeResult forward(RouteConfig routeConfig, HttpServletRequest request) {
        Instant startedAt = Instant.now();
        URI targetUri = buildTargetUri(routeConfig, request);
        String promptPreview = null;

        try {
            byte[] requestBody = readRequestBody(request);
            promptPreview = extractPromptPreview(requestBody, request.getContentType());
            HttpHeaders outboundHeaders = buildOutboundHeaders(routeConfig, request);
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

            WebClient.RequestBodySpec requestSpec = webClient
                    .method(httpMethod)
                    .uri(targetUri)
                    .headers(headers -> headers.addAll(outboundHeaders));

            Mono<ResponseEntity<byte[]>> responseMono;
            if (requestBody.length > 0) {
                responseMono = requestSpec.bodyValue(requestBody).exchangeToMono(response -> response
                        .bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(body -> {
                            HttpHeaders responseHeaders = new HttpHeaders();
                            responseHeaders.putAll(response.headers().asHttpHeaders());
                            responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                            return new ResponseEntity<>(body, responseHeaders, response.statusCode());
                        }));
            } else {
                responseMono = requestSpec.exchangeToMono(response -> response
                        .bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(body -> {
                            HttpHeaders responseHeaders = new HttpHeaders();
                            responseHeaders.putAll(response.headers().asHttpHeaders());
                            responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                            return new ResponseEntity<>(body, responseHeaders, response.statusCode());
                        }));
            }

            ResponseEntity<byte[]> upstreamResponse = responseMono.block(Duration.ofSeconds(120));

            if (upstreamResponse == null) {
                return errorResult(targetUri, startedAt, "Empty response from upstream", promptPreview);
            }

            return new ProxyExchangeResult(
                    upstreamResponse,
                    targetUri,
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    null,
                    promptPreview);
        } catch (Exception ex) {
            return errorResult(targetUri, startedAt, ex.getMessage(), promptPreview);
        }
    }

    private ProxyExchangeResult errorResult(URI targetUri, Instant startedAt, String message, String promptPreview) {
        String body = "Proxy error: " + (message == null ? "unknown" : message);
        ResponseEntity<byte[]> response = ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .header(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
                .body(body.getBytes(StandardCharsets.UTF_8));
        return new ProxyExchangeResult(
                response,
                targetUri,
                Duration.between(startedAt, Instant.now()).toMillis(),
                body,
                promptPreview);
    }

    private URI buildTargetUri(RouteConfig routeConfig, HttpServletRequest request) {
        String incomingPath = request.getRequestURI();
        String routePrefix = routeConfig.getIncomingPrefix();
        String suffix = incomingPath.length() > routePrefix.length()
                ? incomingPath.substring(routePrefix.length())
                : "";

        String target = routeConfig.getTargetBaseUrl() + suffix;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(target);
        if (StringUtils.hasText(request.getQueryString())) {
            builder.query(request.getQueryString());
        }
        return builder.build(true).toUri();
    }

    private HttpHeaders buildOutboundHeaders(RouteConfig routeConfig, HttpServletRequest request) {
        HttpHeaders outboundHeaders = new HttpHeaders();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (HOP_HEADERS.contains(headerName.toLowerCase()) || HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                outboundHeaders.add(headerName, values.nextElement());
            }
        }

        if (routeConfig.getOutboundAuthType() == OutboundAuthType.BEARER
                && StringUtils.hasText(routeConfig.getOutboundBearerToken())) {
            outboundHeaders.setBearerAuth(routeConfig.getOutboundBearerToken());
        }

        return outboundHeaders;
    }

    private byte[] readRequestBody(HttpServletRequest request) throws IOException {
        return StreamUtils.copyToByteArray(request.getInputStream());
    }

    private String extractPromptPreview(byte[] requestBody, String contentType) {
        if (requestBody == null || requestBody.length == 0) {
            return null;
        }
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("json")) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(requestBody);
            if (root == null) {
                return null;
            }

            List<String> chunks = new ArrayList<>();
            JsonNode messagesNode = root.get("messages");
            if (messagesNode != null && messagesNode.isArray()) {
                for (JsonNode message : messagesNode) {
                    String role = textOrNull(message.get("role"));
                    if (role != null && !"user".equalsIgnoreCase(role)) {
                        continue;
                    }
                    collectMessageContent(chunks, message.get("content"));
                }
            }

            if (chunks.isEmpty()) {
                collectMessageContent(chunks, root.get("prompt"));
            }

            String merged = String.join(" ", chunks).replaceAll("\\s+", " ").trim();
            if (merged.isEmpty()) {
                return null;
            }
            return merged.length() > 1000 ? merged.substring(0, 1000) : merged;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void collectMessageContent(List<String> chunks, JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return;
        }
        if (contentNode.isTextual()) {
            chunks.add(contentNode.asText());
            return;
        }
        if (contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    chunks.add(item.asText());
                    continue;
                }
                String text = textOrNull(item.get("text"));
                if (text != null) {
                    chunks.add(text);
                }
            }
            return;
        }
        String text = textOrNull(contentNode.get("text"));
        if (text != null) {
            chunks.add(text);
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    public record ProxyExchangeResult(
            ResponseEntity<byte[]> response,
            URI targetUri,
            long latencyMs,
            String errorMessage,
            String promptPreview) {}
}
