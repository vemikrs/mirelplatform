/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import jp.vemi.mirel.security.jwt.JwtService;

@Configuration
public class AuthenticationProvider {

    @Value("${auth.method:session}")
    private String authMethod;

    @Bean
    public AuthenticationService authenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        AuthenticationService service;
        
        if ("session".equalsIgnoreCase(authMethod)) {
            service = new SessionAuthenticationService(authenticationManager);
        } else {
            service = new JwtAuthenticationService(authenticationManager, jwtService);
        }

        return service;
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) {
        if ("jwt".equalsIgnoreCase(authMethod)) {
            return jwtService.getDecoder();
        }
        return null;
    }
}
