package com.almersoul.llmproxy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "proxy_metric",
        indexes = {
            @Index(name = "idx_proxy_metric_created_at", columnList = "created_at"),
            @Index(name = "idx_proxy_metric_route_name", columnList = "route_name"),
            @Index(name = "idx_proxy_metric_username", columnList = "username")
        })
public class ProxyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 512)
    private String username;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "incoming_path", nullable = false, length = 1000)
    private String incomingPath;

    @Column(name = "target_url", nullable = false, length = 1500)
    private String targetUrl;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "prompt_preview", length = 2000)
    private String promptPreview;

    @ManyToOne(optional = true)
    @JoinColumn(name = "session_id", nullable = true)
    private ProxyUserSession session;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getIncomingPath() {
        return incomingPath;
    }

    public void setIncomingPath(String incomingPath) {
        this.incomingPath = incomingPath;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPromptPreview() {
        return promptPreview;
    }

    public void setPromptPreview(String promptPreview) {
        this.promptPreview = promptPreview;
    }

    public ProxyUserSession getSession() {
        return session;
    }

    public void setSession(ProxyUserSession session) {
        this.session = session;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
