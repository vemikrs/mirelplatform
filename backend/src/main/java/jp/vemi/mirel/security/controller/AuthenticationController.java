/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.security.AuthenticationService;

import java.util.stream.Collectors;

/**
 * 認証コントローラ
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "認証・認可管理API")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // コンストラクタインジェクション
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(
        summary = "ログイン",
        description = "ユーザー名とパスワードで認証を行い、認証トークンを発行します。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "ログイン成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class),
                examples = @ExampleObject(
                    value = "{\"success\":true,\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "認証失敗 - ユーザー名またはパスワードが正しくありません"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ログイン情報",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = AuthenticationRequest.class),
                    examples = @ExampleObject(
                        value = "{\"username\":\"user@example.com\",\"password\":\"password123\"}"
                    )
                )
            )
            @RequestBody AuthenticationRequest request) {
        try {
            String token = authenticationService.authenticate(
                request.getUsername(), 
                request.getPassword()
            );
            
            return ResponseEntity.ok(AuthenticationResponse.builder()
                .success(true)
                .token(token)
                .build());

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthenticationResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build());
        }
    }

    @Operation(
        summary = "認証状態確認",
        description = "現在のユーザーの認証状態を確認します。" +
                      "認証済みの場合、ユーザー名と権限情報を返却します。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "認証状態の取得成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationStatus.class)
            )
        )
    })
    @GetMapping("/check")
    public ResponseEntity<AuthenticationStatus> check() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuthenticationStatus status = AuthenticationStatus.builder()
            .authenticated(false)
            .build();

        if (authentication != null && 
            authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            
            status = AuthenticationStatus.builder()
                .authenticated(true)
                .username(authentication.getName())
                .authorities(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()))
                .build();
        }

        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "ログアウト",
        description = "現在のユーザーセッションを終了し、認証情報をクリアします。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "ログアウト成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationStatus.class),
                examples = @ExampleObject(
                    value = "{\"authenticated\":false,\"message\":\"Logged out successfully\"}"
                )
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<AuthenticationStatus> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return ResponseEntity.ok(AuthenticationStatus.builder()
            .authenticated(false)
            .message("Logged out successfully")
            .build());
    }
}
