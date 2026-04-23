package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.TokenExpiryPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApiTokenCreateRequest {

    @Size(max = 120)
    private String label;

    @NotNull
    private TokenExpiryPolicy expiryPolicy = TokenExpiryPolicy.NEVER;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TokenExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }

    public void setExpiryPolicy(TokenExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
    }
}
