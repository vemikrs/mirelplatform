/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DeviceAuthSession のユニットテスト。
 */
class DeviceAuthSessionTest {
    
    private DeviceAuthSession session;
    
    @BeforeEach
    void setUp() {
        session = DeviceAuthSession.builder()
                .deviceCode("test-device-code")
                .userCode("TEST-CODE")
                .clientId("test-client")
                .scope("api:read api:write")
                .status(DeviceAuthStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900)) // 15分後
                .build();
    }
    
    @Nested
    @DisplayName("isExpired メソッドのテスト")
    class IsExpiredTest {
        
        @Test
        @DisplayName("有効期限内の場合はfalseを返す")
        void shouldReturnFalseWhenNotExpired() {
            assertThat(session.isExpired()).isFalse();
        }
        
        @Test
        @DisplayName("有効期限切れの場合はtrueを返す")
        void shouldReturnTrueWhenExpired() {
            session.setExpiresAt(Instant.now().minusSeconds(1));
            assertThat(session.isExpired()).isTrue();
        }
        
        @Test
        @DisplayName("expiresAtがnullの場合はfalseを返す")
        void shouldReturnFalseWhenExpiresAtIsNull() {
            session.setExpiresAt(null);
            assertThat(session.isExpired()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("isValid メソッドのテスト")
    class IsValidTest {
        
        @Test
        @DisplayName("有効期限内かつPENDING状態の場合はtrueを返す")
        void shouldReturnTrueWhenValidAndPending() {
            assertThat(session.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("期限切れの場合はfalseを返す")
        void shouldReturnFalseWhenExpired() {
            session.setExpiresAt(Instant.now().minusSeconds(1));
            assertThat(session.isValid()).isFalse();
        }
        
        @Test
        @DisplayName("AUTHORIZED状態の場合はfalseを返す")
        void shouldReturnFalseWhenAuthorized() {
            session.setStatus(DeviceAuthStatus.AUTHORIZED);
            assertThat(session.isValid()).isFalse();
        }
        
        @Test
        @DisplayName("DENIED状態の場合はfalseを返す")
        void shouldReturnFalseWhenDenied() {
            session.setStatus(DeviceAuthStatus.DENIED);
            assertThat(session.isValid()).isFalse();
        }
    }
    
    @Test
    @DisplayName("ビルダーで全フィールドが正しく設定される")
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(900);
        
        DeviceAuthSession builtSession = DeviceAuthSession.builder()
                .deviceCode("dc-123")
                .userCode("ABCD-EFGH")
                .clientId("mirel-cli")
                .scope("api:read")
                .status(DeviceAuthStatus.PENDING)
                .userId("user-123")
                .userName("Test User")
                .userEmail("test@example.com")
                .createdAt(now)
                .expiresAt(expiresAt)
                .lastPolledAt(now)
                .build();
        
        assertThat(builtSession.getDeviceCode()).isEqualTo("dc-123");
        assertThat(builtSession.getUserCode()).isEqualTo("ABCD-EFGH");
        assertThat(builtSession.getClientId()).isEqualTo("mirel-cli");
        assertThat(builtSession.getScope()).isEqualTo("api:read");
        assertThat(builtSession.getStatus()).isEqualTo(DeviceAuthStatus.PENDING);
        assertThat(builtSession.getUserId()).isEqualTo("user-123");
        assertThat(builtSession.getUserName()).isEqualTo("Test User");
        assertThat(builtSession.getUserEmail()).isEqualTo("test@example.com");
        assertThat(builtSession.getCreatedAt()).isEqualTo(now);
        assertThat(builtSession.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(builtSession.getLastPolledAt()).isEqualTo(now);
    }
}
