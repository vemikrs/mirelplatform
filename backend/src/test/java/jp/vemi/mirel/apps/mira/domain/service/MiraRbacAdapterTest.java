/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * MiraRbacAdapter のユニットテスト.
 * 
 * <p>
 * ロールベースアクセス制御が正しく機能するかを検証します。
 * </p>
 */
class MiraRbacAdapterTest {

    private MiraRbacAdapter rbacAdapter;

    @BeforeEach
    void setUp() {
        rbacAdapter = new MiraRbacAdapter();
    }

    // ========================================
    // canUseMira() テスト
    // ========================================

    @Nested
    @DisplayName("canUseMira() - Mira利用可否チェック")
    class CanUseMiraTest {

        @ParameterizedTest
        @ValueSource(strings = { "SYSTEM_ADMIN", "ADMIN", "POWER_USER", "STANDARD_USER", "VIEWER" })
        @DisplayName("許可されたロールでMiraを利用可能")
        void shouldAllowMiraForValidRoles(String role) {
            assertThat(rbacAdapter.canUseMira(role, "tenant-1"))
                    .as("ロール %s はMiraを利用可能であるべき", role)
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "system_admin", "admin", "Viewer" })
        @DisplayName("大文字小文字を区別しない")
        void shouldBeCaseInsensitive(String role) {
            assertThat(rbacAdapter.canUseMira(role, "tenant-1"))
                    .as("ロール %s は大文字小文字を区別せずMiraを利用可能であるべき", role)
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "GUEST", "ANONYMOUS", "UNKNOWN" })
        @DisplayName("許可されていないロールではMiraを利用不可")
        void shouldDenyMiraForInvalidRoles(String role) {
            assertThat(rbacAdapter.canUseMira(role, "tenant-1"))
                    .as("ロール %s はMiraを利用不可であるべき", role)
                    .isFalse();
        }

        @Test
        @DisplayName("nullロールではMiraを利用不可")
        void shouldDenyMiraForNullRole() {
            assertThat(rbacAdapter.canUseMira(null, "tenant-1")).isFalse();
        }
    }

    // ========================================
    // canUseMode() テスト
    // ========================================

    @Nested
    @DisplayName("canUseMode() - モード別アクセスチェック")
    class CanUseModeTest {

        @Nested
        @DisplayName("GENERAL_CHAT, CONTEXT_HELP, ERROR_ANALYZE - 全ユーザー利用可")
        class PublicModesTest {

            @ParameterizedTest
            @CsvSource({
                    "GENERAL_CHAT, VIEWER, null",
                    "GENERAL_CHAT, STANDARD_USER, Operator",
                    "CONTEXT_HELP, VIEWER, Viewer",
                    "CONTEXT_HELP, ADMIN, Builder",
                    "ERROR_ANALYZE, VIEWER, null",
                    "ERROR_ANALYZE, POWER_USER, Developer"
            })
            @DisplayName("全ユーザーが利用可能")
            void shouldAllowPublicModesForAllUsers(String modeName, String systemRole, String appRole) {
                MiraMode mode = MiraMode.valueOf(modeName);
                String actualAppRole = "null".equals(appRole) ? null : appRole;

                assertThat(rbacAdapter.canUseMode(mode, systemRole, actualAppRole))
                        .as("モード %s は %s/%s で利用可能であるべき", modeName, systemRole, appRole)
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("STUDIO_AGENT - 開発者以上のみ利用可")
        class StudioAgentTest {

            @ParameterizedTest
            @ValueSource(strings = { "SYSTEM_ADMIN", "ADMIN", "DEVELOPER", "POWER_USER" })
            @DisplayName("許可されたシステムロールで利用可能")
            void shouldAllowStudioAgentForAllowedSystemRoles(String systemRole) {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, systemRole, null))
                        .as("システムロール %s はSTUDIO_AGENTを利用可能であるべき", systemRole)
                        .isTrue();
            }

            @ParameterizedTest
            @ValueSource(strings = { "Developer", "DEVELOPER", "SystemAdmin", "Admin" })
            @DisplayName("許可されたアプリロールで利用可能")
            void shouldAllowStudioAgentForAllowedAppRoles(String appRole) {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, "VIEWER", appRole))
                        .as("アプリロール %s はSTUDIO_AGENTを利用可能であるべき", appRole)
                        .isTrue();
            }

            @Test
            @DisplayName("ViewerはSTUDIO_AGENTを利用不可")
            void shouldDenyStudioAgentForViewer() {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, "VIEWER", null))
                        .as("ViewerはSTUDIO_AGENTを利用不可であるべき")
                        .isFalse();
            }

            @Test
            @DisplayName("STANDARD_USERはSTUDIO_AGENTを利用不可")
            void shouldDenyStudioAgentForStandardUser() {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, "STANDARD_USER", null))
                        .as("STANDARD_USERはSTUDIO_AGENTを利用不可であるべき")
                        .isFalse();
            }

            @Test
            @DisplayName("ViewerでもアプリロールがDeveloperなら利用可能")
            void shouldAllowStudioAgentForViewerWithDeveloperAppRole() {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, "VIEWER", "Developer"))
                        .as("ViewerでもアプリロールがDeveloperならSTUDIO_AGENTを利用可能")
                        .isTrue();
            }

            @Test
            @DisplayName("ViewerでOperator（非開発者）アプリロールでは利用不可")
            void shouldDenyStudioAgentForViewerWithOperatorAppRole() {
                assertThat(rbacAdapter.canUseMode(MiraMode.STUDIO_AGENT, "VIEWER", "Operator"))
                        .as("Viewer + OperatorではSTUDIO_AGENTを利用不可であるべき")
                        .isFalse();
            }
        }

        @Nested
        @DisplayName("WORKFLOW_AGENT - ロールがあれば利用可")
        class WorkflowAgentTest {

            @Test
            @DisplayName("システムロールがあれば利用可能")
            void shouldAllowWorkflowAgentWithSystemRole() {
                assertThat(rbacAdapter.canUseMode(MiraMode.WORKFLOW_AGENT, "VIEWER", null))
                        .isTrue();
            }

            @Test
            @DisplayName("アプリロールのみでも利用可能")
            void shouldAllowWorkflowAgentWithAppRoleOnly() {
                assertThat(rbacAdapter.canUseMode(MiraMode.WORKFLOW_AGENT, null, "Operator"))
                        .isTrue();
            }

            @Test
            @DisplayName("両方nullでは利用不可")
            void shouldDenyWorkflowAgentWithNoRoles() {
                assertThat(rbacAdapter.canUseMode(MiraMode.WORKFLOW_AGENT, null, null))
                        .isFalse();
            }
        }

        @Test
        @DisplayName("nullモードではfalseを返す")
        void shouldReturnFalseForNullMode() {
            assertThat(rbacAdapter.canUseMode(null, "ADMIN", "Builder")).isFalse();
        }
    }

    // ========================================
    // canExportAuditLog() テスト
    // ========================================

    @Nested
    @DisplayName("canExportAuditLog() - 監査ログエクスポート権限チェック")
    class CanExportAuditLogTest {

        @ParameterizedTest
        @ValueSource(strings = { "SYSTEM_ADMIN", "ADMIN", "AUDITOR" })
        @DisplayName("許可されたロールでエクスポート可能")
        void shouldAllowAuditExportForAllowedRoles(String role) {
            assertThat(rbacAdapter.canExportAuditLog(role))
                    .as("ロール %s は監査ログエクスポート可能であるべき", role)
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "system_admin", "admin", "Auditor" })
        @DisplayName("大文字小文字を区別しない")
        void shouldBeCaseInsensitive(String role) {
            assertThat(rbacAdapter.canExportAuditLog(role)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "VIEWER", "STANDARD_USER", "POWER_USER", "DEVELOPER" })
        @DisplayName("許可されていないロールではエクスポート不可")
        void shouldDenyAuditExportForUnauthorizedRoles(String role) {
            assertThat(rbacAdapter.canExportAuditLog(role))
                    .as("ロール %s は監査ログエクスポート不可であるべき", role)
                    .isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null/空文字ではエクスポート不可")
        void shouldDenyAuditExportForNullOrEmpty(String role) {
            assertThat(rbacAdapter.canExportAuditLog(role)).isFalse();
        }
    }

    // ========================================
    // canViewAllConversations() テスト
    // ========================================

    @Nested
    @DisplayName("canViewAllConversations() - 全会話閲覧権限チェック")
    class CanViewAllConversationsTest {

        @ParameterizedTest
        @ValueSource(strings = { "SYSTEM_ADMIN", "ADMIN" })
        @DisplayName("管理者は全会話を閲覧可能")
        void shouldAllowAdminsToViewAllConversations(String role) {
            assertThat(rbacAdapter.canViewAllConversations(role)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "system_admin", "admin" })
        @DisplayName("大文字小文字を区別しない")
        void shouldBeCaseInsensitive(String role) {
            assertThat(rbacAdapter.canViewAllConversations(role)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = { "VIEWER", "STANDARD_USER", "POWER_USER", "DEVELOPER", "AUDITOR" })
        @DisplayName("非管理者は全会話を閲覧不可")
        void shouldDenyNonAdminsFromViewingAllConversations(String role) {
            assertThat(rbacAdapter.canViewAllConversations(role)).isFalse();
        }

        @Test
        @DisplayName("nullロールでは閲覧不可")
        void shouldDenyNullRole() {
            assertThat(rbacAdapter.canViewAllConversations(null)).isFalse();
        }
    }

    // ========================================
    // canViewUserConversation() テスト
    // ========================================

    @Nested
    @DisplayName("canViewUserConversation() - ユーザー会話閲覧権限チェック")
    class CanViewUserConversationTest {

        @Test
        @DisplayName("自分の会話は常に閲覧可能")
        void shouldAlwaysAllowViewingOwnConversation() {
            assertThat(rbacAdapter.canViewUserConversation("VIEWER", "user-1", "user-1"))
                    .isTrue();
        }

        @Test
        @DisplayName("管理者は他ユーザーの会話を閲覧可能")
        void shouldAllowAdminToViewOtherUsersConversation() {
            assertThat(rbacAdapter.canViewUserConversation("ADMIN", "admin-1", "user-2"))
                    .isTrue();
        }

        @Test
        @DisplayName("非管理者は他ユーザーの会話を閲覧不可")
        void shouldDenyNonAdminFromViewingOtherUsersConversation() {
            assertThat(rbacAdapter.canViewUserConversation("VIEWER", "user-1", "user-2"))
                    .isFalse();
        }

        @Test
        @DisplayName("リクエストユーザーIDがnullでも管理者なら閲覧可能")
        void shouldAllowAdminEvenWithNullRequestUserId() {
            assertThat(rbacAdapter.canViewUserConversation("SYSTEM_ADMIN", null, "user-2"))
                    .isTrue();
        }
    }
}
