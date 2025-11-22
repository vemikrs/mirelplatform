/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

import jp.vemi.framework.util.DatabaseUtil;
import jp.vemi.mirel.config.properties.Mipla2SecurityProperties;
import jp.vemi.mirel.security.AuthenticationService;
import jp.vemi.mirel.foundation.service.oauth2.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${auth.method:jwt}")
    private String authMethod;

    @Autowired
    private Mipla2SecurityProperties securityProperties;
    
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    /**
     * Spring Securityのセキュリティフィルタチェーンを構成します。
     * CSRF保護、認可設定、認証方式の設定を行います。
     * 
     * <p>
     * このメソッドは以下の設定を行います：
     * <ul>
     * <li>CSRF保護の設定
     * <li>URLパターンごとのアクセス制御
     * <li>JWT認証またはフォーム認証の設定
     * </ul>
     *
     * @param http
     *            セキュリティ設定を構成するためのビルダー
     * @param authenticationService
     *            認証サービス（JWT認証またはフォーム認証の実装）
     * @return 構成されたセキュリティフィルタチェーン
     * @throws Exception
     *             セキュリティ設定の構成中にエラーが発生した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationService authenticationService) throws Exception {
        DatabaseUtil.initializeDefaultTenant();

        configureCors(http);
        configureCsrf(http);
        configureAuthorization(http);
        configureAuthentication(http, authenticationService);

        return http.build();
    }

    /**
     * CORS設定を行います。
     * 開発環境でフロントエンドからのAPIアクセスを許可します。
     *
     * @param http セキュリティ設定
     * @throws Exception 設定中に例外が発生した場合
     */
    private void configureCors(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    }

    /**
     * CORS設定ソースを提供します。
     * 
     * @return CORS設定ソース
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * CSRFの設定を行います。
     * securityPropertiesの設定に応じてCSRF保護の有効/無効を切り替えます。
     *
     * @param http
     *            セキュリティ設定
     * @throws Exception
     *             設定中に例外が発生した場合
     */
    // CodeQL [java/spring-disabled-csrf-protection] - CSRF protection is configurable via Mipla2SecurityProperties
    // Default is ENABLED (csrfEnabled=true). Development mode can disable via application-dev.yml for testing.
    // This is intentional design for flexibility in different environments.
    @SuppressWarnings({"lgtm[java/spring-disabled-csrf-protection]"})
    private void configureCsrf(HttpSecurity http) throws Exception {
        // NOTE: CSRF protection is enabled by default (Mipla2SecurityProperties.csrfEnabled=true)
        // Development environment can override this setting in application-dev.yml
        http.csrf(csrf -> {
            if (!securityProperties.isCsrfEnabled()) {
                csrf.disable();
            } else {
                csrf.ignoringRequestMatchers(
                        "/auth/**",
                        "/api/**",
                        "/apps/*/api/**",
                        "/login/oauth2/code/**",  // OAuth2コールバックをCSRF除外
                        "/oauth2/**")             // OAuth2認証エンドポイントをCSRF除外
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
            }
        });
    }

    /**
     * 認可設定を行います。
     * securityPropertiesの設定に応じてAPIエンドポイントの認可要否を制御します。
     *
     * @param http
     *            セキュリティ設定
     * @throws Exception
     *             設定中に例外が発生した場合
     */
    private void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> {
            // 共通パブリックエンドポイント
            authz.requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/auth/check").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/framework/db/**").permitAll() // Debug DB access endpoint
                    .requestMatchers("/v3/api-docs/**").permitAll() // OpenAPI JSON endpoint
                    .requestMatchers("/api-docs/**").permitAll() // OpenAPI JSON endpoint(Legacy)
                    .requestMatchers("/swagger-ui/**").permitAll() // Swagger UI static resources
                    .requestMatchers("/swagger-ui.html").permitAll(); // Swagger UI HTML

            // セキュリティ無効時は全てのAPIをパブリックに
            if (!securityProperties.isEnabled()) {
                authz.requestMatchers("/commons/**").permitAll()
                    .requestMatchers("/apps/*/api/**").permitAll()
                    .anyRequest().permitAll(); // ゲストモード：全てのリクエストを許可
            } else {
                // セキュリティ有効時のみ認証を要求
                authz.anyRequest().authenticated();
            }
        });
    }

    /**
     * 認証設定を行います。
     * authMethodの設定に応じてJWT認証またはフォーム認証を設定します。
     *
     * @param http
     *            セキュリティ設定
     * @param authenticationService
     *            認証サービス
     * @throws Exception
     *             設定中に例外が発生した場合
     */
    private void configureAuthentication(HttpSecurity http, AuthenticationService authenticationService)
            throws Exception {
        if ("jwt".equals(authMethod) && authenticationService.isJwtSupported()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(authenticationService.getJwtDecoder())))
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        } else {
            // JWT無効時はカスタム認証エンドポイント (/auth/login) を使用するため、
            // formLogin を設定せず、セッション管理のみ設定
            http.sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        }
        
        // OAuth2ログイン設定（GitHub）
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService))
                .loginPage("/login")
                .defaultSuccessUrl("/auth/oauth2/success", true)
                .failureUrl("/login?error=oauth2"));
    }

    /**
     * デフォルトのユーザー詳細サービスを提供します。
     * 開発環境用の基本認証ユーザーを設定します。
     *
     * @param passwordEncoder
     *            パスワードエンコーダー
     * @return UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username("dev")
                .password(passwordEncoder.encode("dev"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder)
            throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.inMemoryAuthentication()
                .withUser("dev").password(passwordEncoder.encode("dev")).roles("USER");
        return authenticationManagerBuilder.build();
    }
}
