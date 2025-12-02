/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.config;

import jakarta.annotation.PostConstruct;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SystemUser Migration Component
 * 
 * 既存のSystemUserレコードにusernameが設定されていない場合、
 * emailから自動生成してマイグレーションします。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemUserMigration {
    
    private final SystemUserRepository systemUserRepository;
    
    @PostConstruct
    @Transactional
    public void migrateSystemUsers() {
        log.info("Starting SystemUser migration: checking for missing usernames");
        
        List<SystemUser> usersWithoutUsername = systemUserRepository.findAll().stream()
                .filter(user -> user.getUsername() == null || user.getUsername().isEmpty())
                .toList();
        
        if (usersWithoutUsername.isEmpty()) {
            log.info("No SystemUser records require username migration");
            return;
        }
        
        log.info("Found {} SystemUser records without username, migrating...", usersWithoutUsername.size());
        
        for (SystemUser user : usersWithoutUsername) {
            String username = generateUsernameFromEmail(user.getEmail());
            
            // ユニーク制約を考慮して重複チェック
            String finalUsername = ensureUniqueUsername(username);
            
            user.setUsername(finalUsername);
            systemUserRepository.save(user);
            
            log.info("Migrated SystemUser: id={}, email={}, username={}", 
                    user.getId(), user.getEmail(), finalUsername);
        }
        
        log.info("SystemUser migration completed: {} records updated", usersWithoutUsername.size());
    }
    
    /**
     * メールアドレスからusernameを生成
     * 
     * @param email メールアドレス
     * @return username
     */
    private String generateUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        
        return email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");
    }
    
    /**
     * ユニークなusernameを確保（重複時は数字サフィックスを追加）
     * 
     * @param baseUsername ベースとなるusername
     * @return ユニークなusername
     */
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int suffix = 1;
        
        while (systemUserRepository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
        }
        
        return username;
    }
}
