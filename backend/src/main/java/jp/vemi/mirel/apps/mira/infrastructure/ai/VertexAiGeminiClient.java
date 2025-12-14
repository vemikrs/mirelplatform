/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.core.io.FileSystemResource;
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

            // オプション生成 (リクエスト単位で上書き)
            VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                    .model(properties.getVertexAi().getModel())
                    .temperature(request.getTemperature() != null ? request.getTemperature()
                            : properties.getVertexAi().getTemperature())
                    .googleSearchRetrieval(request.isGoogleSearchRetrieval())
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
            log.error("[VertexAiGemini] Request failed: {}", e.getMessage(), e);
            return AiResponse.error("REQUEST_FAILED",
                    "Vertex AI エラー: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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

        // オプション生成
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(properties.getVertexAi().getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature()
                        : properties.getVertexAi().getTemperature())
                .googleSearchRetrieval(request.isGoogleSearchRetrieval())
                .build();

        Prompt prompt = new Prompt(messages, options);

        return client.prompt(prompt)
                .stream()
                .chatResponse()
                .map(this::mapStreamResponse)
                .onErrorResume(e -> {
                    log.error("[VertexAiGemini] Stream failed", e);
                    return Flux.just(AiResponse.error("STREAM_ERROR", "Stream error: " + e.getMessage()));
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
            if (response.getResult().getMetadata() != null && response.getResult().getMetadata().getFinishReason() != null) {
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
     * @param msg AIリクエストメッセージ
     * @return マルチモーダルUserMessage
     */
    private UserMessage createMultimodalUserMessage(AiRequest.Message msg) {

        try {

            final List<Media> media = msg.getAttachedFiles().stream().map(attachedFile -> {

                try {
                String filePath = getFilePathFromFileId(attachedFile.getFileId());
                if (filePath == null) {
                    log.warn("File not found for fileId: {}", attachedFile.getFileId());
                    return null;
                }
                FileSystemResource resource = new FileSystemResource(filePath);
                if (!resource.exists()) {
                    log.warn("File does not exist: {}", filePath);
                    return null;
                }

                return new Media(MimeTypeUtils.parseMimeType(attachedFile.getMimeType()), resource);

            } catch (Exception e) {
                log.error("Failed to load file: {}", attachedFile.getFileId(), e);
                return null;
            }
        }).filter(mediaItem -> mediaItem != null).toList();

        return UserMessage.builder()
                .text(msg.getContent())
                .media(media)
                .build();

        } catch (Exception e) {
            log.error("Failed to create multimodal user message: {}",  e);
            return new UserMessage(msg.getContent());
        }
    }
    
    /**
     * FileIDからファイルパスを取得.
     * 
     * @param fileId ファイルID
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
}
