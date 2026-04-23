package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.ApiToken;
import com.almersoul.llmproxy.domain.TokenExpiryPolicy;
import java.time.Instant;

public record ApiTokenResponse(
        Long id,
        String label,
        String tokenPrefix,
        TokenExpiryPolicy expiryPolicy,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        Instant createdAt,
        ApiTokenStatus status) {

    public static ApiTokenResponse from(ApiToken token, Instant now) {
        ApiTokenStatus status = token.isRevoked()
                ? ApiTokenStatus.REVOKED
                : (token.isExpiredAt(now) ? ApiTokenStatus.EXPIRED : ApiTokenStatus.ACTIVE);
        return new ApiTokenResponse(
                token.getId(),
                token.getLabel(),
                token.getTokenPrefix(),
                token.getExpiryPolicy(),
                token.getExpiresAt(),
                token.getRevokedAt(),
                token.getLastUsedAt(),
                token.getCreatedAt(),
                status);
    }
}
