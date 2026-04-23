package com.almersoul.llmproxy.dto;

public record ApiTokenCreateResponse(
        String plainToken,
        ApiTokenResponse token) {}
