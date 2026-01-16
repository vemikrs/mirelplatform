/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import reactor.util.retry.Retry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import com.google.cloud.vertexai.VertexAI;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Vertex AI (Gemini) プロバイダクライアント.
 * 
 * <p>
 * Spring AI の VertexAiGeminiChatModel を使用して実装しますが、
 * 自動構成との競合を避けるために手動で ChatModel を構築します。
 * </p>
 */
@Slf4j
@Component
public class VertexAiGeminiClient implements AiProviderClient {

    private static final String PROVIDER_NAME = "vertex-ai-gemini";

    private final MiraAiProperties properties;
    private final VertexAiGeminiChatModel chatModel;

    @Autowired(required = false)
    private jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository fileManagementRepository;

    @Autowired(required = false)
    private jp.vemi.framework.storage.StorageService storageService;

    public VertexAiGeminiClient(MiraAiProperties properties) {
        this.properties = properties;
        // プロジェクトIDが未設定の場合は初期化しない（isAvailable()で判定）
        if (properties.getVertexAi().getProjectId() == null || properties.getVertexAi().getProjectId().isEmpty()) {
            this.chatModel = null;
            log.warn("Vertex AI Project ID is not configured. VertexAiGeminiClient will be disabled.");
        } else {
            this.chatModel = buildChatModel();
        }
    }

    private VertexAiGeminiChatModel buildChatModel() {
        var config = properties.getVertexAi();

        log.info("Initializing VertexAiGeminiClient with project: {}, location: {}, model: {}",
                config.getProjectId(), config.getLocation(), config.getModel());

        // Vertex AI API クライアントの生成
        VertexAI vertexAi = new VertexAI(config.getProjectId(), config.getLocation());

        // オプション生成
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature())
                .build();

        // ChatModel生成
        return VertexAiGeminiChatModel.builder()
                .vertexAI(vertexAi)
                .defaultOptions(options)
                .build();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        if (this.chatModel == null) {
            return AiResponse.error("PROVIDER_NOT_CONFIGURED", "Vertex AI is not configured.");
        }

        long startTime = System.currentTimeMillis();

        try {
            // ChatClient の準備
            ChatClient client = ChatClient.builder(this.chatModel)
                    .build();

            // メッセージの変換
            List<Message> messages = request.getMessages().stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());

            // マルチモーダル入力がある場合はGoogle Search Groundingを無効化 (API制約/安定性のため)
            boolean hasMedia = request.getMessages().stream()
                    .anyMatch(m -> m.getAttachedFiles() != null && !m.getAttachedFiles().isEmpty());

            boolean enableGrounding = request.isGoogleSearchRetrieval();
            if (hasMedia && enableGrounding) {
                log.warn("Disabling Google Search Grounding because multimodal input is present.");
                enableGrounding = false;
            }

            // オプション生成 (リクエスト単位で上書き)
            VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                    .model(properties.getVertexAi().getModel())
                    .temperature(request.getTemperature() != null ? request.getTemperature()
                            : properties.getVertexAi().getTemperature())
                    .googleSearchRetrieval(enableGrounding)
                    .build();

            Prompt prompt = new Prompt(messages, options);

            // TODO: ToolCallback / Function Call 対応は必要に応じて実装
            // 現状はシンプルチャットのみ

            // 実行
            ChatResponse response = client.prompt(prompt)
                    .call()
                    .chatResponse();

            long latency = System.currentTimeMillis() - startTime;

            if (response.getResult() == null) {
                return AiResponse.error("EMPTY_RESPONSE", "No result from Gemini");
            }

            String content = response.getResult().getOutput().getText();

            AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                    .model(properties.getVertexAi().getModel())
                    .latencyMs(latency)
                    .build();

            return AiResponse.success(content, metadata);

        } catch (Exception e) {
            log.error("[VertexAiGemini] Request failed", e);
            // エラー詳細はログに記録し、ユーザーには簡潔なメッセージのみ表示
            return AiResponse.error("REQUEST_FAILED",
                    "AI の応答生成に失敗しました。しばらくしてから再度お試しください。");
        }
    }

    @Override
    public Flux<AiResponse> stream(AiRequest request) {
        if (this.chatModel == null) {
            return Flux.just(AiResponse.error("PROVIDER_NOT_CONFIGURED", "Vertex AI is not configured."));
        }

        // ChatClient の準備
        ChatClient client = ChatClient.builder(this.chatModel)
                .build();

        // メッセージの変換
        List<Message> messages = request.getMessages().stream()
                .map(this::mapMessage)
                .collect(Collectors.toList());

        // マルチモーダル入力がある場合はGoogle Search Groundingを無効化
        boolean hasMedia = request.getMessages().stream()
                .anyMatch(m -> m.getAttachedFiles() != null && !m.getAttachedFiles().isEmpty());

        boolean enableGrounding = request.isGoogleSearchRetrieval();
        if (hasMedia && enableGrounding) {
            log.warn("Disabling Google Search Grounding because multimodal input is present.");
            enableGrounding = false;
        }

        // オプション生成
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(properties.getVertexAi().getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature()
                        : properties.getVertexAi().getTemperature())
                .googleSearchRetrieval(enableGrounding)
                .build();

        Prompt prompt = new Prompt(messages, options);

        return client.prompt(prompt)
                .stream()
                .chatResponse()
                .map(this::mapStreamResponse)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(VertexAiGeminiClient::shouldRetryException)
                        .doBeforeRetry(
                                retrySignal -> log.warn("[VertexAiGemini] Retrying stream request (attempt {}): {}",
                                        retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .onErrorResume(e -> {
                    log.error("[VertexAiGemini] Stream failed after retries", e);
                    if (e instanceof io.grpc.StatusRuntimeException) {
                        return Flux.error(new jp.vemi.framework.exeption.MirelSystemException(
                                "Google AI API error: " + e.getMessage(), e));
                    }
                    return Flux.error(
                            new jp.vemi.framework.exeption.MirelSystemException("Stream error: " + e.getMessage(), e));
                });
    }

    private AiResponse mapStreamResponse(ChatResponse response) {
        if (response.getResult() == null) {
            log.warn("[VertexAiGemini] Stream response result is null: {}", response);
            return AiResponse.builder().content("").build();
        }

        String content = "";
        if (response.getResult().getOutput() != null && response.getResult().getOutput().getText() != null) {
            content = response.getResult().getOutput().getText();
        } else {
            String finishReason = "unknown";
            if (response.getResult().getMetadata() != null
                    && response.getResult().getMetadata().getFinishReason() != null) {
                finishReason = response.getResult().getMetadata().getFinishReason();
            }
            log.warn("[VertexAiGemini] Stream response text is null. FinishReason: {}", finishReason);
        }

        AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                .model(properties.getVertexAi().getModel())
                .build();

        return AiResponse.builder()
                .content(content)
                .metadata(metadata)
                .build();
    }

    private Message mapMessage(AiRequest.Message msg) {
        switch (msg.getRole()) {
            case "user":
                // ファイル添付がある場合はマルチモーダル入力として処理
                if (msg.getAttachedFiles() != null && !msg.getAttachedFiles().isEmpty()) {
                    return createMultimodalUserMessage(msg);
                }
                return new UserMessage(msg.getContent());
            case "system":
                return new SystemMessage(msg.getContent());
            case "assistant":
                return new AssistantMessage(msg.getContent());
            default:
                return new UserMessage(msg.getContent());
        }
    }

    /**
     * マルチモーダルユーザーメッセージを作成.
     * 
     * @param msg
     *            AIリクエストメッセージ
     * @return マルチモーダルUserMessage
     */
    private UserMessage createMultimodalUserMessage(AiRequest.Message msg) {

        try {

            log.info("Creating multimodal message with {} attached files", msg.getAttachedFiles().size());
            final List<Media> media = msg.getAttachedFiles().stream().map(attachedFile -> {

                try {
                    log.debug("Loading file: fileId={}, mimeType={}", attachedFile.getFileId(),
                            attachedFile.getMimeType());
                    String storagePath = getFilePathFromFileId(attachedFile.getFileId());
                    if (storagePath == null) {
                        log.warn("File not found for fileId: {}", attachedFile.getFileId());
                        return null;
                    }

                    // StorageService経由でファイル読み込み
                    if (storageService == null) {
                        log.warn("StorageService is not available");
                        return null;
                    }
                    if (!storageService.exists(storagePath)) {
                        log.warn("File does not exist in storage: {}", storagePath);
                        return null;
                    }

                    byte[] fileBytes = storageService.getBytes(storagePath);
                    ByteArrayResource resource = new ByteArrayResource(fileBytes);
                    log.debug("File loaded successfully from storage: {} ({} bytes)", storagePath, fileBytes.length);

                    return new Media(MimeTypeUtils.parseMimeType(attachedFile.getMimeType()), resource);

                } catch (Exception e) {
                    log.error("Failed to load file: {}", attachedFile.getFileId(), e);
                    return null;
                }
            }).filter(java.util.Objects::nonNull).toList();

            return UserMessage.builder()
                    .text(msg.getContent())
                    .media(media)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create multimodal user message", e);
            // ファイル読み込み失敗時はテキストのみで送信
            log.warn("Falling back to text-only message due to file processing error");
            return new UserMessage(msg.getContent());
        }
    }

    /**
     * FileIDからファイルパスを取得.
     * 
     * @param fileId
     *            ファイルID
     * @return ファイルパス
     */
    private String getFilePathFromFileId(String fileId) {
        if (fileManagementRepository == null) {
            log.warn("FileManagementRepository is not available");
            return null;
        }

        return fileManagementRepository.findById(fileId)
                .map(fm -> fm.getFilePath())
                .orElse(null);
    }

    @Override
    public boolean isAvailable() {
        return this.chatModel != null;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * リトライ対象かどうかを判定.
     * 
     * <p>
     * 認証・権限系エラーはリトライ対象外（設定問題のため即時失敗が適切）
     * </p>
     * 
     * @param throwable
     *            例外
     * @return true: リトライ対象, false: リトライ対象外
     */
    static boolean shouldRetryException(Throwable throwable) {
        if (throwable instanceof io.grpc.StatusRuntimeException) {
            io.grpc.Status.Code code = ((io.grpc.StatusRuntimeException) throwable).getStatus().getCode();
            if (code == io.grpc.Status.Code.PERMISSION_DENIED ||
                    code == io.grpc.Status.Code.UNAUTHENTICATED ||
                    code == io.grpc.Status.Code.INVALID_ARGUMENT) {
                log.warn("[VertexAiGemini] Non-retryable error: {}", code);
                return false;
            }
        }
        return true;
    }
}
