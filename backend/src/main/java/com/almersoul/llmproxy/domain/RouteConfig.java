package com.almersoul.llmproxy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "proxy_route_config")
public class RouteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "incoming_prefix", nullable = false, unique = true, length = 300)
    private String incomingPrefix;

    @Column(name = "target_base_url", nullable = false, length = 800)
    private String targetBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbound_auth_type", nullable = false, length = 32)
    private OutboundAuthType outboundAuthType = OutboundAuthType.NONE;

    @Column(name = "outbound_bearer_token", length = 2048)
    private String outboundBearerToken;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIncomingPrefix() {
        return incomingPrefix;
    }

    public void setIncomingPrefix(String incomingPrefix) {
        this.incomingPrefix = incomingPrefix;
    }

    public String getTargetBaseUrl() {
        return targetBaseUrl;
    }

    public void setTargetBaseUrl(String targetBaseUrl) {
        this.targetBaseUrl = targetBaseUrl;
    }

    public OutboundAuthType getOutboundAuthType() {
        return outboundAuthType;
    }

    public void setOutboundAuthType(OutboundAuthType outboundAuthType) {
        this.outboundAuthType = outboundAuthType;
    }

    public String getOutboundBearerToken() {
        return outboundBearerToken;
    }

    public void setOutboundBearerToken(String outboundBearerToken) {
        this.outboundBearerToken = outboundBearerToken;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
