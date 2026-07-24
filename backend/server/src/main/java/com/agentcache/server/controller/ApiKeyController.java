package com.agentcache.server.controller;

import com.agentcache.application.service.ApiKeyService;
import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.application.service.ApiKeyService.CreateApiKeyOutcome;
import com.agentcache.common.response.Result;
import com.agentcache.server.dto.ApiKeyResponse;
import com.agentcache.server.dto.CreateApiKeyRequest;
import com.agentcache.server.dto.CreateApiKeyResponse;
import com.agentcache.server.security.AuthenticatedActor;
import com.agentcache.server.security.RequestActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API Key 控制器。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final RequestActorResolver actorResolver;

    @GetMapping("/spaces/{spaceId}/api-keys")
    public Result<List<ApiKeyResponse>> list(@PathVariable Long spaceId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(apiKeyService.listBySpace(spaceId, caller).stream()
                .map(ApiKeyResponse::from)
                .toList());
    }

    @PostMapping("/spaces/{spaceId}/api-keys")
    public Result<CreateApiKeyResponse> create(@PathVariable Long spaceId,
                                               @Valid @RequestBody CreateApiKeyRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        CreateApiKeyOutcome outcome = apiKeyService.createApiKey(spaceId, caller, request.getName(), request.getRole());
        CreateApiKeyResponse response = new CreateApiKeyResponse(
                outcome.apiKey().getId(),
                outcome.plainKey(),
                outcome.apiKey().getName(),
                outcome.apiKey().getRole(),
                outcome.apiKey().getCreatedAt());
        return Result.success(response);
    }

    @DeleteMapping("/api-keys/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam Long spaceId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        apiKeyService.revokeApiKey(spaceId, id, caller);
        return Result.success();
    }

    static AuthenticatedCaller toCaller(AuthenticatedActor actor) {
        return new AuthenticatedCaller(actor.getUserId(), actor.getKind() == AuthenticatedActor.Kind.API_KEY,
                actor.getSpaceId(), actor.getSpaceRole());
    }
}