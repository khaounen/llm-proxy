package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.OutboundAuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RouteConfigRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 300)
    private String incomingPrefix;

    @NotBlank
    @Size(max = 800)
    private String targetBaseUrl;

    @NotNull
    private OutboundAuthType outboundAuthType = OutboundAuthType.NONE;

    @Size(max = 2048)
    private String outboundBearerToken;

    private boolean active = true;

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
}
