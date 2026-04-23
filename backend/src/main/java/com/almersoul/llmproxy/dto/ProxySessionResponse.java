package com.almersoul.llmproxy.dto;

import com.almersoul.llmproxy.domain.ProxyUserSession;
import java.time.Instant;

public record ProxySessionResponse(
        Long id,
        String username,
        Instant startedAt,
        Instant lastPromptAt,
        long promptCount) {

    public static ProxySessionResponse from(ProxyUserSession session) {
        return new ProxySessionResponse(
                session.getId(),
                session.getUsername(),
                session.getStartedAt(),
                session.getLastPromptAt(),
                session.getPromptCount());
    }
}
