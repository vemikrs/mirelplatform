/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JWT の roles クレームを Spring Security の GrantedAuthority に変換するコンバーター。
 * 
 * <p>JWT トークンに含まれる "roles" クレームを解析し、
 * Spring Security の認可処理で使用可能な GrantedAuthority のコレクションに変換します。</p>
 * 
 * <p>対応するロール形式:
 * <ul>
 *   <li>"ROLE_ADMIN" - そのまま使用</li>
 *   <li>"ADMIN" - "ROLE_" プレフィックスを自動付与</li>
 * </ul>
 * </p>
 * 
 * @see org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
 */
@Component
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Object rolesObj = jwt.getClaim(ROLES_CLAIM);
        if (rolesObj instanceof List<?>) {
            List<?> rolesList = (List<?>) rolesObj;
            for (Object role : rolesList) {
                if (role instanceof String roleStr) {
                    // ROLE_ プレフィックスがない場合は追加
                    if (!roleStr.startsWith(ROLE_PREFIX)) {
                        roleStr = ROLE_PREFIX + roleStr;
                    }
                    authorities.add(new SimpleGrantedAuthority(roleStr));
                }
                // null やその他の型は無視
            }
        }

        return authorities;
    }
}
