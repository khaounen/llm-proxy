package com.almersoul.llmproxy.service;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class AuthenticatedUserResolver {

    public AuthenticatedUser resolve(Principal principal) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authenticated user is required");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            String issuer = jwt.getIssuer() == null ? "unknown-issuer" : jwt.getIssuer().toString();
            String subject = sanitize(jwt.getSubject(), jwtAuthenticationToken.getName());
            String username = sanitize(
                    jwt.getClaimAsString("preferred_username"),
                    sanitize(jwt.getClaimAsString("email"), jwtAuthenticationToken.getName()));
            return new AuthenticatedUser(issuer, subject, username);
        }

        String username = sanitize(authentication.getName(), "unknown-user");
        return new AuthenticatedUser("local-basic", username, username);
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
