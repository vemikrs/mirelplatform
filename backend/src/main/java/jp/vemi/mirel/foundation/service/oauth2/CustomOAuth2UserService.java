/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service.oauth2;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom OAuth2UserService
 * 
 * GitHub OAuth2認証後のユーザー情報を処理し、SystemUserと紐付けます。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final SystemUserRepository systemUserRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * OAuth2ユーザー情報を取得し、SystemUserを作成または更新します。
     * 
     * @param userRequest OAuth2ユーザー情報リクエスト
     * @return OAuth2User
     * @throws OAuth2AuthenticationException OAuth2認証エラー
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 login attempt: provider={}", registrationId);
        
        // GitHub OAuth2ユーザー情報を抽出
        if ("github".equals(registrationId)) {
            GitHubOAuth2UserInfo userInfo = GitHubOAuth2UserInfo.from(oauth2User.getAttributes());
            processGitHubUser(userInfo);
        }
        
        return oauth2User;
    }
    
    /**
     * GitHubユーザー情報を処理し、SystemUserを作成または更新します。
     * 
     * @param userInfo GitHub OAuth2ユーザー情報
     */
    private void processGitHubUser(GitHubOAuth2UserInfo userInfo) {
        String providerId = String.valueOf(userInfo.getId());
        String provider = "github";
        
        // OAuth2プロバイダーIDで既存ユーザーを検索
        Optional<SystemUser> existingUser = systemUserRepository
                .findByOauth2ProviderAndOauth2ProviderId(provider, providerId);
        
        if (existingUser.isPresent()) {
            // 既存ユーザーの情報を更新
            SystemUser user = existingUser.get();
            updateSystemUserFromGitHub(user, userInfo);
            systemUserRepository.save(user);
            log.info("Updated existing OAuth2 user: id={}, email={}", user.getId(), user.getEmail());
        } else {
            // メールアドレスで既存ユーザーを検索（メール連携）
            Optional<SystemUser> userByEmail = userInfo.getEmail() != null
                    ? systemUserRepository.findByEmail(userInfo.getEmail())
                    : Optional.empty();
            
            if (userByEmail.isPresent()) {
                // 既存メールアドレスのユーザーにOAuth2情報を紐付け
                SystemUser user = userByEmail.get();
                user.setOauth2Provider(provider);
                user.setOauth2ProviderId(providerId);
                updateSystemUserFromGitHub(user, userInfo);
                systemUserRepository.save(user);
                log.info("Linked OAuth2 to existing email user: id={}, email={}", user.getId(), user.getEmail());
            } else {
                // 新規ユーザーを作成
                SystemUser newUser = createSystemUserFromGitHub(userInfo, provider, providerId);
                systemUserRepository.save(newUser);
                log.info("Created new OAuth2 user: id={}, email={}", newUser.getId(), newUser.getEmail());
            }
        }
    }
    
    /**
     * GitHub OAuth2情報から新規SystemUserを作成します。
     * 
     * @param userInfo GitHub OAuth2ユーザー情報
     * @param provider OAuth2プロバイダー名
     * @param providerId OAuth2プロバイダーID
     * @return 新規SystemUser
     */
    private SystemUser createSystemUserFromGitHub(GitHubOAuth2UserInfo userInfo, String provider, String providerId) {
        SystemUser user = new SystemUser();
        user.setId(UUID.randomUUID());
        
        // ユーザー名: GitHubログイン名を使用
        user.setUsername(userInfo.getLogin());
        
        // メールアドレス: GitHubから取得（nullの場合は仮のアドレス）
        String email = userInfo.getEmail() != null
                ? userInfo.getEmail()
                : userInfo.getLogin() + "@github.oauth2.local";
        user.setEmail(email);
        
        // パスワード: OAuth2ログインではパスワード不要だが、
        // エンティティのnullable=false制約のためランダム値を設定
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        
        // メール検証済み（GitHubで検証済みと仮定）
        user.setEmailVerified(userInfo.getEmail() != null);
        
        // アバターURL
        user.setAvatarUrl(userInfo.getAvatarUrl());
        
        // OAuth2情報
        user.setOauth2Provider(provider);
        user.setOauth2ProviderId(providerId);
        
        // アカウント有効化
        user.setIsActive(true);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        
        return user;
    }
    
    /**
     * 既存SystemUserをGitHub OAuth2情報で更新します。
     * 
     * @param user 既存SystemUser
     * @param userInfo GitHub OAuth2ユーザー情報
     */
    private void updateSystemUserFromGitHub(SystemUser user, GitHubOAuth2UserInfo userInfo) {
        // アバターURLを更新（最新の画像を反映）
        user.setAvatarUrl(userInfo.getAvatarUrl());
        
        // メールアドレスが変更されている場合は更新（nullチェック）
        if (userInfo.getEmail() != null && !userInfo.getEmail().equals(user.getEmail())) {
            user.setEmail(userInfo.getEmail());
            user.setEmailVerified(true);
        }
    }
}
