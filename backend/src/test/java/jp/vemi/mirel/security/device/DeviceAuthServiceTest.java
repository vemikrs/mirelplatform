/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.security.device.dto.DeviceCodeRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeResponse;
import jp.vemi.mirel.security.device.dto.DeviceTokenResponse;
import jp.vemi.mirel.security.jwt.JwtService;

/**
 * DeviceAuthService のユニットテスト。
 */
@ExtendWith(MockitoExtension.class)
class DeviceAuthServiceTest {
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private DeviceAuthService deviceAuthService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deviceAuthService, "appBaseUrl", "http://localhost:5173");
    }
    
    @Nested
    @DisplayName("createDeviceCode メソッドのテスト")
    class CreateDeviceCodeTest {
        
        @Test
        @DisplayName("デバイスコードとユーザーコードが正しく生成される")
        void shouldCreateDeviceCodeSuccessfully() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read api:write")
                    .build();
            
            DeviceCodeResponse response = deviceAuthService.createDeviceCode(request);
            
            assertThat(response).isNotNull();
            assertThat(response.getDeviceCode()).isNotBlank();
            assertThat(response.getUserCode()).matches("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
            assertThat(response.getVerificationUri()).isEqualTo("http://localhost:5173/cli/auth");
            assertThat(response.getExpiresIn()).isEqualTo(900);
            assertThat(response.getInterval()).isEqualTo(5);
        }
        
        @Test
        @DisplayName("ユーザーコードに紛らわしい文字が含まれない")
        void shouldNotContainConfusingCharacters() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            
            // 複数回生成して確認
            for (int i = 0; i < 100; i++) {
                DeviceCodeResponse response = deviceAuthService.createDeviceCode(request);
                String userCode = response.getUserCode().replace("-", "");
                
                assertThat(userCode).doesNotContain("0", "O", "1", "I", "L");
            }
        }
    }
    
    @Nested
    @DisplayName("getSessionByDeviceCode メソッドのテスト")
    class GetSessionByDeviceCodeTest {
        
        @Test
        @DisplayName("存在するデバイスコードでセッションを取得できる")
        void shouldReturnSessionForExistingDeviceCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse response = deviceAuthService.createDeviceCode(request);
            
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByDeviceCode(response.getDeviceCode());
            
            assertThat(session).isPresent();
            assertThat(session.get().getClientId()).isEqualTo("mirel-cli");
        }
        
        @Test
        @DisplayName("存在しないデバイスコードの場合はemptyを返す")
        void shouldReturnEmptyForNonExistingDeviceCode() {
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByDeviceCode("non-existing");
            
            assertThat(session).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("getSessionByUserCode メソッドのテスト")
    class GetSessionByUserCodeTest {
        
        @Test
        @DisplayName("存在するユーザーコードでセッションを取得できる")
        void shouldReturnSessionForExistingUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse response = deviceAuthService.createDeviceCode(request);
            
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByUserCode(response.getUserCode());
            
            assertThat(session).isPresent();
            assertThat(session.get().getDeviceCode()).isEqualTo(response.getDeviceCode());
        }
        
        @Test
        @DisplayName("存在しないユーザーコードの場合はemptyを返す")
        void shouldReturnEmptyForNonExistingUserCode() {
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByUserCode("XXXX-XXXX");
            
            assertThat(session).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("pollToken メソッドのテスト")
    class PollTokenTest {
        
        @Test
        @DisplayName("PENDING状態の場合はpendingレスポンスを返す")
        void shouldReturnPendingWhenSessionIsPending() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            DeviceTokenResponse response = deviceAuthService.pollToken(createResponse.getDeviceCode(), "mirel-cli");
            
            assertThat(response.getStatus()).isEqualTo("pending");
            assertThat(response.getAccessToken()).isNull();
        }
        
        @Test
        @DisplayName("存在しないデバイスコードの場合はexpiredレスポンスを返す")
        void shouldReturnExpiredWhenDeviceCodeNotFound() {
            DeviceTokenResponse response = deviceAuthService.pollToken("non-existing", "mirel-cli");
            
            assertThat(response.getStatus()).isEqualTo("expired");
        }
        
        @Test
        @DisplayName("クライアントIDが一致しない場合はエラーを返す")
        void shouldReturnErrorWhenClientIdMismatch() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            DeviceTokenResponse response = deviceAuthService.pollToken(createResponse.getDeviceCode(), "wrong-client");
            
            assertThat(response.getError()).isEqualTo("invalid_client");
        }
        
        @Test
        @DisplayName("ポーリング間隔が短すぎる場合はslow_downエラーを返す")
        void shouldReturnSlowDownWhenPollingTooFast() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            // 1回目のポーリング
            deviceAuthService.pollToken(createResponse.getDeviceCode(), "mirel-cli");
            
            // すぐに2回目のポーリング
            DeviceTokenResponse response = deviceAuthService.pollToken(createResponse.getDeviceCode(), "mirel-cli");
            
            assertThat(response.getError()).isEqualTo("slow_down");
        }
        
        @Test
        @DisplayName("DENIED状態の場合はdeniedレスポンスを返す")
        void shouldReturnDeniedWhenSessionIsDenied() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            // 拒否
            deviceAuthService.deny(createResponse.getUserCode());
            
            DeviceTokenResponse response = deviceAuthService.pollToken(createResponse.getDeviceCode(), "mirel-cli");
            
            assertThat(response.getStatus()).isEqualTo("denied");
        }
        
        @Test
        @DisplayName("AUTHORIZED状態の場合はトークンを含むレスポンスを返す")
        void shouldReturnTokenWhenSessionIsAuthorized() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            // モック設定
            User mockUser = new User();
            mockUser.setUserId("user-123");
            mockUser.setRoles("USER");
            when(userRepository.findById("user-123")).thenReturn(Optional.of(mockUser));
            when(jwtService.generateCliToken(eq("user-123"), eq("api:read"), eq("mirel-cli"), anyList()))
                    .thenReturn("mock-jwt-token");
            
            // 承認
            deviceAuthService.authorize(createResponse.getUserCode(), "user-123", "Test User", "test@example.com");
            
            DeviceTokenResponse response = deviceAuthService.pollToken(createResponse.getDeviceCode(), "mirel-cli");
            
            assertThat(response.getStatus()).isEqualTo("authorized");
            assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(86400);
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
            assertThat(response.getUser().getName()).isEqualTo("Test User");
        }
    }
    
    @Nested
    @DisplayName("authorize メソッドのテスト")
    class AuthorizeTest {
        
        @Test
        @DisplayName("有効なユーザーコードを承認できる")
        void shouldAuthorizeValidUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            boolean result = deviceAuthService.authorize(
                    createResponse.getUserCode(), 
                    "user-123", 
                    "Test User", 
                    "test@example.com"
            );
            
            assertThat(result).isTrue();
            
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByUserCode(createResponse.getUserCode());
            assertThat(session).isPresent();
            assertThat(session.get().getStatus()).isEqualTo(DeviceAuthStatus.AUTHORIZED);
            assertThat(session.get().getUserId()).isEqualTo("user-123");
        }
        
        @Test
        @DisplayName("存在しないユーザーコードの場合はfalseを返す")
        void shouldReturnFalseForNonExistingUserCode() {
            boolean result = deviceAuthService.authorize("XXXX-XXXX", "user-123", "Test", "test@example.com");
            
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("既に承認済みのユーザーコードの場合はfalseを返す")
        void shouldReturnFalseForAlreadyAuthorizedUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            // 1回目の承認
            deviceAuthService.authorize(createResponse.getUserCode(), "user-123", "Test", "test@example.com");
            
            // 2回目の承認
            boolean result = deviceAuthService.authorize(createResponse.getUserCode(), "user-456", "Test2", "test2@example.com");
            
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("deny メソッドのテスト")
    class DenyTest {
        
        @Test
        @DisplayName("有効なユーザーコードを拒否できる")
        void shouldDenyValidUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            boolean result = deviceAuthService.deny(createResponse.getUserCode());
            
            assertThat(result).isTrue();
            
            Optional<DeviceAuthSession> session = deviceAuthService.getSessionByUserCode(createResponse.getUserCode());
            assertThat(session).isPresent();
            assertThat(session.get().getStatus()).isEqualTo(DeviceAuthStatus.DENIED);
        }
        
        @Test
        @DisplayName("存在しないユーザーコードの場合はfalseを返す")
        void shouldReturnFalseForNonExistingUserCode() {
            boolean result = deviceAuthService.deny("XXXX-XXXX");
            
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("validateUserCode メソッドのテスト")
    class ValidateUserCodeTest {
        
        @Test
        @DisplayName("有効なユーザーコードの場合はセッションを返す")
        void shouldReturnSessionForValidUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            
            Optional<DeviceAuthSession> session = deviceAuthService.validateUserCode(createResponse.getUserCode());
            
            assertThat(session).isPresent();
        }
        
        @Test
        @DisplayName("承認済みのユーザーコードの場合はemptyを返す")
        void shouldReturnEmptyForAuthorizedUserCode() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            DeviceCodeResponse createResponse = deviceAuthService.createDeviceCode(request);
            deviceAuthService.authorize(createResponse.getUserCode(), "user-123", "Test", "test@example.com");
            
            Optional<DeviceAuthSession> session = deviceAuthService.validateUserCode(createResponse.getUserCode());
            
            assertThat(session).isEmpty();
        }
    }
}
