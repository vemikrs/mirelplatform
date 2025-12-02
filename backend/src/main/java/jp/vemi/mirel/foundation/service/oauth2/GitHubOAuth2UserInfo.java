/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service.oauth2;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

/**
 * GitHub OAuth2ユーザー情報
 * 
 * GitHub APIから取得したユーザー属性を格納します。
 */
@Getter
@Setter
public class GitHubOAuth2UserInfo {
    
    /**
     * GitHub user ID
     */
    private Long id;
    
    /**
     * GitHub login (username)
     */
    private String login;
    
    /**
     * Name (display name)
     */
    private String name;
    
    /**
     * Email address
     */
    private String email;
    
    /**
     * Avatar image URL
     */
    private String avatarUrl;
    
    /**
     * GitHub attributes から GitHubOAuth2UserInfo を構築
     * 
     * @param attributes OAuth2User attributes
     * @return GitHubOAuth2UserInfo インスタンス
     */
    public static GitHubOAuth2UserInfo from(Map<String, Object> attributes) {
        GitHubOAuth2UserInfo userInfo = new GitHubOAuth2UserInfo();
        
        userInfo.setId(getLong(attributes, "id"));
        userInfo.setLogin(getString(attributes, "login"));
        userInfo.setName(getString(attributes, "name"));
        userInfo.setEmail(getString(attributes, "email"));
        userInfo.setAvatarUrl(getString(attributes, "avatar_url"));
        
        return userInfo;
    }
    
    private static String getString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
    
    private static Long getLong(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
