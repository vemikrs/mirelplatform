/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.controller;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PUBLIC)
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AuthenticationStatus {
    private boolean authenticated;
    private String username;
    private java.util.List<String> authorities;
    private String message;
}