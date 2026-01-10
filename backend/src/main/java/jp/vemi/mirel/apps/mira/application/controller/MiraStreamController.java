/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.service.MiraAuditService;
import jp.vemi.mirel.apps.mira.domain.service.MiraRbacAdapter;
import jp.vemi.mirel.apps.mira.domain.service.MiraStreamResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraStreamService;
import jp.vemi.mirel.apps.mira.domain.service.MiraTenantContextManager;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;
import reactor.core.publisher.Flux;

/**
 * Mira AI Streams API Controller.
 */
@Slf4j
@RestController
@RequestMapping("apps/mira/api/stream")
@RequiredArgsConstructor
@Tag(name = "Mira AI Stream", description = "AI Realtime Streaming API")
public class MiraStreamController {

    /** エラーコードの最大長 */
    private static final int MAX_ERROR_CODE_LENGTH = 36;

    private final MiraStreamService streamService;
    private final MiraTenantContextManager tenantContextManager;
    private final MiraRbacAdapter rbacAdapter;
    private final MiraAuditService auditService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chat Stream", description = "Stream AI responses")
    public Flux<MiraStreamResponse> chatStream(
            @RequestBody ApiRequest<ChatRequest> request) {

        // Context
        String tenantId = tenantContextManager.getCurrentTenantId();
        String userId = tenantContextManager.getCurrentUserId();
        String systemRole = tenantContextManager.getCurrentSystemRole();

        // RBAC Check
        if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
            log.warn("Mira Stream Access Denied: tenantId={}, userId={}", tenantId, userId);
            return Flux.just(MiraStreamResponse.error("FORBIDDEN", "Mira access denied"));
        }

        ChatRequest chatRequest = request.getModel();
        if (chatRequest == null) {
            log.error("ChatRequest is null in ApiRequest");
            return Flux.just(MiraStreamResponse.error("INVALID_REQUEST", "Invalid request format"));
        }
        log.info("Stream Chat Request Received: RagEnabled={}, Mode={}", chatRequest.getRagEnabled(),
                SanitizeUtil.forLog(chatRequest.getMode()));

        try {
            return streamService.streamChat(chatRequest, tenantId, userId);
        } catch (Exception e) {
            log.error("Stream initialization error", e);
            // エラーメッセージを最大長以内に制限
            String errorCode = e.getClass().getSimpleName();
            if (errorCode.length() > MAX_ERROR_CODE_LENGTH) {
                errorCode = errorCode.substring(0, MAX_ERROR_CODE_LENGTH);
            }
            auditService.logApiError(tenantId, userId, "stream/chat", errorCode);
            return Flux.just(MiraStreamResponse.error("INTERNAL_ERROR", "Server error during stream init"));
        }
    }
}
