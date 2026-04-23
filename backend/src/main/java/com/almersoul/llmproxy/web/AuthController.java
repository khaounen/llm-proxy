package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.dto.AuthMeResponse;
import com.almersoul.llmproxy.service.AuthenticatedUser;
import com.almersoul.llmproxy.service.AuthenticatedUserResolver;
import java.security.Principal;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AuthController(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/me")
    public AuthMeResponse me(Principal principal) {
        AuthenticatedUser user = authenticatedUserResolver.resolve(principal);
        Authentication authentication = (Authentication) principal;
        List<String> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .sorted()
                .toList();

        String authType = resolveAuthType(authentication);
        return new AuthMeResponse(
                user.username(),
                user.issuer(),
                user.subject(),
                authType,
                roles);
    }

    private String resolveAuthType(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            return "OIDC";
        }
        if (authentication instanceof UsernamePasswordAuthenticationToken token) {
            boolean isProxyToken = token.getAuthorities().stream()
                    .anyMatch(granted -> "ROLE_PROXY_USER".equals(granted.getAuthority()));
            return isProxyToken ? "API_TOKEN" : "BASIC";
        }
        return authentication.getClass().getSimpleName();
    }
}
