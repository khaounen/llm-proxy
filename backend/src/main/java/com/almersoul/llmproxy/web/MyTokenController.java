package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.dto.ApiTokenCreateRequest;
import com.almersoul.llmproxy.dto.ApiTokenCreateResponse;
import com.almersoul.llmproxy.dto.ApiTokenExpiryUpdateRequest;
import com.almersoul.llmproxy.dto.ApiTokenResponse;
import com.almersoul.llmproxy.service.ApiTokenService;
import com.almersoul.llmproxy.service.AuthenticatedUser;
import com.almersoul.llmproxy.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/tokens")
public class MyTokenController {

    private final ApiTokenService apiTokenService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public MyTokenController(
            ApiTokenService apiTokenService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.apiTokenService = apiTokenService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public List<ApiTokenResponse> list(Principal principal) {
        AuthenticatedUser user = authenticatedUserResolver.resolve(principal);
        return apiTokenService.listForUser(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiTokenCreateResponse create(
            Principal principal,
            @Valid @RequestBody ApiTokenCreateRequest request) {
        AuthenticatedUser user = authenticatedUserResolver.resolve(principal);
        return apiTokenService.createForUser(user, request);
    }

    @PatchMapping("/{id}/expiry")
    public ApiTokenResponse updateExpiry(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ApiTokenExpiryUpdateRequest request) {
        AuthenticatedUser user = authenticatedUserResolver.resolve(principal);
        return apiTokenService.updateExpiry(user, id, request.getExpiryPolicy());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Principal principal, @PathVariable Long id) {
        AuthenticatedUser user = authenticatedUserResolver.resolve(principal);
        apiTokenService.revoke(user, id);
    }
}
