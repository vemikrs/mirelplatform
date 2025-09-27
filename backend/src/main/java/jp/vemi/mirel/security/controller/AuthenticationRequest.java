/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.controller;

/**
 * ログインリクエスト
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PUBLIC)
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AuthenticationRequest {
    private String username;
    private String password;
}
