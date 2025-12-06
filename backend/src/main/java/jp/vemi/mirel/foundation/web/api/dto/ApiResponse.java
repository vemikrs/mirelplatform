/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.foundation.web.api.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * APIレスポンスのクラスです .<br/>
 */
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@lombok.Builder
public class ApiResponse<T> {

    /** レスポンスデータ */
    private T data;

    /** メッセージリスト */
    @lombok.Builder.Default
    private List<String> messages = new ArrayList<>();

    /** エラーメッセージリスト */
    @lombok.Builder.Default
    private List<String> errors = new ArrayList<>();

    /** エラー追加 */
    public void addError(String message) {
        this.errors.add(message);
    }

    /** メッセージ追加 */
    public void addMessage(String message) {
        this.messages.add(message);
    }

    /** エラーリスト追加 */
    public void addErrors(List<String> messages) {
        this.errors.addAll(messages);
    }

    /** メッセージリスト追加 */
    public void addMessages(List<String> messages) {
        this.messages.addAll(messages);
    }

    /** データ設定コンストラクタ */
    public ApiResponse(T data) {
        this.data = data;
    }

    /**
     * 成功レスポンス生成
     * 
     * @param <T>
     *            データ型
     * @param data
     *            データ
     * @return レスポンス
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    /**
     * 成功レスポンス生成（データなし）
     * 
     * @return レスポンス
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null);
    }
}
