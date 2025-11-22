/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import jp.vemi.mirel.security.jwt.JwtService;

@Configuration
public class AuthenticationProvider {

    @Bean
    @ConditionalOnProperty(name = "auth.jwt.enabled", havingValue = "true", matchIfMissing = false)
    public JwtDecoder jwtDecoder(@Autowired(required = false) JwtService jwtService) {
        return jwtService != null ? jwtService.getDecoder() : null;
    }
}
