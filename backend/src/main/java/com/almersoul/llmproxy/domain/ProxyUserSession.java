package com.almersoul.llmproxy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "proxy_user_session",
        indexes = {
            @Index(name = "idx_proxy_user_session_username", columnList = "username"),
            @Index(name = "idx_proxy_user_session_last_prompt_at", columnList = "last_prompt_at")
        })
public class ProxyUserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String username;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "last_prompt_at", nullable = false)
    private Instant lastPromptAt;

    @Column(name = "prompt_count", nullable = false)
    private long promptCount;

    public void start(String username, Instant startedAt) {
        this.username = username;
        this.startedAt = startedAt;
        this.lastPromptAt = startedAt;
        this.promptCount = 0L;
    }

    public void registerPrompt(Instant at) {
        this.lastPromptAt = at;
        this.promptCount += 1;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastPromptAt() {
        return lastPromptAt;
    }

    public long getPromptCount() {
        return promptCount;
    }
}
