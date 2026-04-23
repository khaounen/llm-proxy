package com.almersoul.llmproxy.service;

import com.almersoul.llmproxy.domain.ApiToken;
import com.almersoul.llmproxy.domain.TokenExpiryPolicy;
import com.almersoul.llmproxy.dto.ApiTokenCreateRequest;
import com.almersoul.llmproxy.dto.ApiTokenCreateResponse;
import com.almersoul.llmproxy.dto.ApiTokenResponse;
import com.almersoul.llmproxy.repository.ApiTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ApiTokenService {

    private static final String TOKEN_PREFIX = "lp_";
    private static final int TOKEN_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = 14;

    private final ApiTokenRepository apiTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiTokenService(ApiTokenRepository apiTokenRepository) {
        this.apiTokenRepository = apiTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<ApiTokenResponse> listForUser(AuthenticatedUser user) {
        Instant now = Instant.now();
        return apiTokenRepository.findByIssuerAndSubjectOrderByCreatedAtDesc(user.issuer(), user.subject()).stream()
                .map(token -> ApiTokenResponse.from(token, now))
                .toList();
    }

    @Transactional
    public ApiTokenCreateResponse createForUser(AuthenticatedUser user, ApiTokenCreateRequest request) {
        Instant now = Instant.now();
        TokenMaterial tokenMaterial = newTokenMaterial();

        ApiToken token = new ApiToken();
        token.setLabel(normalizeLabel(request.getLabel()));
        token.setTokenPrefix(tokenMaterial.displayPrefix());
        token.setTokenHash(tokenMaterial.hash());
        token.setIssuer(user.issuer());
        token.setSubject(user.subject());
        token.setUsername(user.username());
        applyExpiry(token, request.getExpiryPolicy(), now);
        ApiToken saved = apiTokenRepository.save(token);

        return new ApiTokenCreateResponse(tokenMaterial.plainToken(), ApiTokenResponse.from(saved, now));
    }

    @Transactional
    public ApiTokenResponse updateExpiry(AuthenticatedUser user, Long tokenId, TokenExpiryPolicy expiryPolicy) {
        ApiToken token = findOwnedToken(user, tokenId);
        if (token.isRevoked()) {
            throw new ResponseStatusException(BAD_REQUEST, "Token is revoked");
        }
        Instant now = Instant.now();
        applyExpiry(token, expiryPolicy, now);
        return ApiTokenResponse.from(apiTokenRepository.save(token), now);
    }

    @Transactional
    public void revoke(AuthenticatedUser user, Long tokenId) {
        ApiToken token = findOwnedToken(user, tokenId);
        if (!token.isRevoked()) {
            token.setRevokedAt(Instant.now());
            apiTokenRepository.save(token);
        }
    }

    @Transactional
    public Optional<AuthenticatedUser> authenticate(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hashToken(plainToken);
        ApiToken token = apiTokenRepository.findByTokenHash(hash).orElse(null);
        if (token == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (token.isRevoked() || token.isExpiredAt(now)) {
            return Optional.empty();
        }
        token.setLastUsedAt(now);
        apiTokenRepository.save(token);
        return Optional.of(new AuthenticatedUser(token.getIssuer(), token.getSubject(), token.getUsername()));
    }

    private ApiToken findOwnedToken(AuthenticatedUser user, Long tokenId) {
        return apiTokenRepository
                .findByIdAndIssuerAndSubject(tokenId, user.issuer(), user.subject())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Token not found"));
    }

    private void applyExpiry(ApiToken token, TokenExpiryPolicy expiryPolicy, Instant now) {
        TokenExpiryPolicy effectivePolicy = expiryPolicy == null ? TokenExpiryPolicy.NEVER : expiryPolicy;
        token.setExpiryPolicy(effectivePolicy);
        switch (effectivePolicy) {
            case NEVER -> token.setExpiresAt(null);
            case P30D -> token.setExpiresAt(now.plus(30, ChronoUnit.DAYS));
            case P365D -> token.setExpiresAt(now.plus(365, ChronoUnit.DAYS));
        }
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return null;
        }
        String normalized = label.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TokenMaterial newTokenMaterial() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String plainToken = TOKEN_PREFIX + randomPart;
        String displayPrefix = plainToken.substring(0, Math.min(DISPLAY_PREFIX_LENGTH, plainToken.length()));
        return new TokenMaterial(plainToken, displayPrefix, hashToken(plainToken));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private record TokenMaterial(String plainToken, String displayPrefix, String hash) {}
}
