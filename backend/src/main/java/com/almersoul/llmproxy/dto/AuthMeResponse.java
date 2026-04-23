package com.almersoul.llmproxy.dto;

import java.util.List;

public record AuthMeResponse(
        String username,
        String issuer,
        String subject,
        String authenticationType,
        List<String> roles) {}
