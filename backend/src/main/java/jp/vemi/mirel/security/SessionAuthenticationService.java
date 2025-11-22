/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "auth.jwt.enabled", havingValue = "false", matchIfMissing = true)
public class SessionAuthenticationService implements AuthenticationService {

    private final AuthenticationManager authenticationManager;

    public SessionAuthenticationService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public String authenticate(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "Login successful";
    }

    @Override
    public JwtDecoder getJwtDecoder() {
        return null; // Session-based authentication doesn't use JWT
    }

    @Override
    public boolean isJwtSupported() {
        return false; // This implementation uses session-based authentication
    }
}
