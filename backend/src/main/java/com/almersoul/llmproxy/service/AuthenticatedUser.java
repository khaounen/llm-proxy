package com.almersoul.llmproxy.service;

public record AuthenticatedUser(String issuer, String subject, String username) {

    public String key() {
        return issuer + "|" + subject;
    }
}
