/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.FeatureFlagRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.admin.dto.CreateFeatureFlagRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminFeatureFlagController のテスト
 */
@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "mirel.security.enabled=true"  // セキュリティを有効化してテスト
})
@AutoConfigureMockMvc
@Transactional
class AdminFeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        // テスト用ユーザー作成
        adminUser = new User();
        adminUser.setUserId("test-admin-" + UUID.randomUUID().toString().substring(0, 8));
        adminUser.setUsername("testadmin");
        adminUser.setEmail("testadmin@example.com");
        adminUser.setRoles("ADMIN|USER");
        adminUser.setIsActive(true);
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUserId("test-user-" + UUID.randomUUID().toString().substring(0, 8));
        regularUser.setUsername("testuser");
        regularUser.setEmail("testuser@example.com");
        regularUser.setRoles("USER");
        regularUser.setIsActive(true);
        userRepository.save(regularUser);
    }

    @Nested
    @DisplayName("認可テスト")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールでアクセス可能")
        @WithMockUser(roles = "ADMIN")
        void adminCanAccessFeatures() throws Exception {
            mockMvc.perform(get("/admin/features")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("USERロールではアクセス拒否")
        @WithMockUser(roles = "USER")
        void userCannotAccessFeatures() throws Exception {
            mockMvc.perform(get("/admin/features")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証ではアクセス拒否")
        void anonymousCannotAccessFeatures() throws Exception {
            mockMvc.perform(get("/admin/features")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("フィーチャーフラグ一覧取得")
    class ListFeaturesTest {

        @Test
        @DisplayName("ページネーション付きで一覧取得")
        @WithMockUser(roles = "ADMIN")
        void listFeaturesWithPagination() throws Exception {
            // テストデータ作成
            createTestFeatureFlag("test-feature-1", "Test Feature 1");
            createTestFeatureFlag("test-feature-2", "Test Feature 2");

            mockMvc.perform(get("/admin/features")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @DisplayName("ステータスでフィルタリング")
        @WithMockUser(roles = "ADMIN")
        void listFeaturesWithStatusFilter() throws Exception {
            createTestFeatureFlag("stable-feature", "Stable Feature", FeatureFlag.FeatureStatus.STABLE);
            createTestFeatureFlag("beta-feature", "Beta Feature", FeatureFlag.FeatureStatus.BETA);

            mockMvc.perform(get("/admin/features")
                    .param("page", "0")
                    .param("size", "10")
                    .param("status", "STABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray());
        }
    }

    @Nested
    @DisplayName("フィーチャーフラグ作成")
    class CreateFeatureTest {

        @Test
        @DisplayName("ADMINが新規フィーチャーフラグを作成できる")
        @WithMockUser(username = "test-admin", roles = "ADMIN")
        void adminCanCreateFeature() throws Exception {
            CreateFeatureFlagRequest request = new CreateFeatureFlagRequest();
            request.setFeatureKey("new.test.feature");
            request.setFeatureName("New Test Feature");
            request.setApplicationId("promarker");
            request.setStatus(FeatureFlag.FeatureStatus.PLANNING);

            mockMvc.perform(post("/admin/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureKey").value("new.test.feature"))
                .andExpect(jsonPath("$.featureName").value("New Test Feature"));

            // DBに保存されたことを確認
            assertThat(featureFlagRepository.findByFeatureKeyAndDeleteFlagFalse("new.test.feature")).isPresent();
        }

        @Test
        @DisplayName("重複するfeatureKeyは作成できない")
        @WithMockUser(username = "test-admin", roles = "ADMIN")
        void cannotCreateDuplicateFeatureKey() throws Exception {
            // 既存のフィーチャーを作成
            createTestFeatureFlag("duplicate.key", "Existing Feature");

            CreateFeatureFlagRequest request = new CreateFeatureFlagRequest();
            request.setFeatureKey("duplicate.key");
            request.setFeatureName("Duplicate Feature");
            request.setApplicationId("promarker");

            mockMvc.perform(post("/admin/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("キー存在チェック")
    class CheckKeyExistsTest {

        @Test
        @DisplayName("存在するキーの場合trueを返す")
        @WithMockUser(roles = "ADMIN")
        void checkExistingKey() throws Exception {
            createTestFeatureFlag("existing.key", "Existing Feature");

            mockMvc.perform(get("/admin/features/check-key")
                    .param("featureKey", "existing.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
        }

        @Test
        @DisplayName("存在しないキーの場合falseを返す")
        @WithMockUser(roles = "ADMIN")
        void checkNonExistingKey() throws Exception {
            mockMvc.perform(get("/admin/features/check-key")
                    .param("featureKey", "non.existing.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
        }
    }

    // ヘルパーメソッド
    private FeatureFlag createTestFeatureFlag(String key, String name) {
        return createTestFeatureFlag(key, name, FeatureFlag.FeatureStatus.STABLE);
    }

    private FeatureFlag createTestFeatureFlag(String key, String name, FeatureFlag.FeatureStatus status) {
        FeatureFlag flag = new FeatureFlag();
        flag.setId("test-" + UUID.randomUUID().toString().substring(0, 8));
        flag.setFeatureKey(key);
        flag.setFeatureName(name);
        flag.setApplicationId("promarker");
        flag.setStatus(status);
        flag.setEnabledByDefault(true);
        flag.setRolloutPercentage(100);
        flag.setInDevelopment(false);
        return featureFlagRepository.save(flag);
    }
}
