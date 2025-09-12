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

import jp.vemi.framework.util.DatabaseUtil;
import jp.vemi.mirel.config.properties.Mipla2SecurityProperties;
import jp.vemi.mirel.security.AuthenticationService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${auth.method:jwt}")
    private String authMethod;

    @Autowired
    private Mipla2SecurityProperties securityProperties;

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

        configureCsrf(http);
        configureAuthorization(http);
        configureAuthentication(http, authenticationService);

        return http.build();
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
    private void configureCsrf(HttpSecurity http) throws Exception {
        http.csrf(csrf -> {
            if (!securityProperties.isCsrfEnabled()) {
                csrf.disable();
            } else {
                csrf.ignoringRequestMatchers(
                        "/auth/**",
                        "/api/**",
                        "/apps/*/api/**")
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
            if (!securityProperties.isEnabled()) {
                authz.requestMatchers("/apps/*/api/**").permitAll();
            }

            authz.requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/auth/check").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .anyRequest().authenticated();
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
            http.formLogin(form -> form
                    .loginPage("/auth/login")
                    .permitAll())
                    .logout(logout -> logout
                            .logoutUrl("/auth/logout")
                            .permitAll());
        }
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
