/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.security.jwt.JwtService;

@Service
@ConditionalOnProperty(name = "auth.method", havingValue = "jwt", matchIfMissing = true)
public class JwtAuthenticationService implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public JwtAuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public String authenticate(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        return jwtService.generateToken(authentication);
    }

    @Override
    public boolean isJwtSupported() {
        return true;
    }

    @Override
    public JwtDecoder getJwtDecoder() {
        return jwtService.getDecoder();
    }
}
