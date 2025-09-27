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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.security.AuthenticationService;

import java.util.stream.Collectors;

/**
 * 認証コントローラ
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // コンストラクタインジェクション
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
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
