package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.TokenExpiryPolicy;
import jakarta.validation.constraints.NotNull;

public class ApiTokenExpiryUpdateRequest {

    @NotNull
    private TokenExpiryPolicy expiryPolicy;

    public TokenExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }

    public void setExpiryPolicy(TokenExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
    }
}
