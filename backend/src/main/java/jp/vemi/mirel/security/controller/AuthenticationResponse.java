/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.controller;

/**
 * ログインレスポンス
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PUBLIC)
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AuthenticationResponse {
    private boolean success;
    private String token;
    private String message;
}
