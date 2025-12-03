/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.security.device.dto.DeviceAuthorizationRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeResponse;
import jp.vemi.mirel.security.device.dto.DeviceTokenRequest;
import jp.vemi.mirel.security.device.dto.DeviceTokenResponse;

/**
 * DeviceAuthController のユニットテスト。
 * Spring コンテキストを使わない軽量テスト。
 */
@ExtendWith(MockitoExtension.class)
class DeviceAuthControllerTest {
    
    @Mock
    private DeviceAuthService deviceAuthService;
    
    @Mock
    private ExecutionContext executionContext;
    
    @InjectMocks
    private DeviceAuthController controller;
    
    @Nested
    @DisplayName("POST /api/auth/device/code - デバイスコード発行")
    class RequestDeviceCodeTest {
        
        @Test
        @DisplayName("正常にデバイスコードを発行できる")
        void shouldCreateDeviceCodeSuccessfully() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("mirel-cli")
                    .scope("api:read api:write")
                    .build();
            
            DeviceCodeResponse expectedResponse = DeviceCodeResponse.builder()
                    .deviceCode("test-device-code")
                    .userCode("ABCD-EFGH")
                    .verificationUri("http://localhost:5173/cli/auth")
                    .expiresIn(900)
                    .interval(5)
                    .build();
            
            when(deviceAuthService.createDeviceCode(any())).thenReturn(expectedResponse);
            
            ResponseEntity<DeviceCodeResponse> response = controller.requestDeviceCode(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getDeviceCode()).isEqualTo("test-device-code");
            assertThat(response.getBody().getUserCode()).isEqualTo("ABCD-EFGH");
            assertThat(response.getBody().getExpiresIn()).isEqualTo(900);
            assertThat(response.getBody().getInterval()).isEqualTo(5);
        }
        
        @Test
        @DisplayName("クライアントIDが空の場合はBad Requestを返す")
        void shouldReturnBadRequestWhenClientIdIsEmpty() {
            DeviceCodeRequest request = DeviceCodeRequest.builder()
                    .clientId("")
                    .scope("api:read")
                    .build();
            
            ResponseEntity<DeviceCodeResponse> response = controller.requestDeviceCode(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
    
    @Nested
    @DisplayName("POST /api/auth/device/token - トークンポーリング")
    class PollTokenTest {
        
        @Test
        @DisplayName("PENDING状態のレスポンスを返す")
        void shouldReturnPendingResponse() {
            DeviceTokenRequest request = DeviceTokenRequest.builder()
                    .clientId("mirel-cli")
                    .deviceCode("test-device-code")
                    .build();
            
            when(deviceAuthService.pollToken("test-device-code", "mirel-cli"))
                    .thenReturn(DeviceTokenResponse.pending());
            
            ResponseEntity<DeviceTokenResponse> response = controller.pollToken(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo("pending");
        }
        
        @Test
        @DisplayName("レート制限エラーの場合は429を返す")
        void shouldReturn429WhenSlowDown() {
            DeviceTokenRequest request = DeviceTokenRequest.builder()
                    .clientId("mirel-cli")
                    .deviceCode("test-device-code")
                    .build();
            
            when(deviceAuthService.pollToken("test-device-code", "mirel-cli"))
                    .thenReturn(DeviceTokenResponse.slowDown());
            
            ResponseEntity<DeviceTokenResponse> response = controller.pollToken(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody().getError()).isEqualTo("slow_down");
        }
        
        @Test
        @DisplayName("認証完了時はトークンを含むレスポンスを返す")
        void shouldReturnTokenWhenAuthorized() {
            DeviceTokenRequest request = DeviceTokenRequest.builder()
                    .clientId("mirel-cli")
                    .deviceCode("test-device-code")
                    .build();
            
            DeviceTokenResponse expectedResponse = DeviceTokenResponse.builder()
                    .status("authorized")
                    .accessToken("jwt-token")
                    .tokenType("Bearer")
                    .expiresIn(86400)
                    .user(DeviceTokenResponse.UserInfo.builder()
                            .email("test@example.com")
                            .name("Test User")
                            .build())
                    .build();
            
            when(deviceAuthService.pollToken("test-device-code", "mirel-cli"))
                    .thenReturn(expectedResponse);
            
            ResponseEntity<DeviceTokenResponse> response = controller.pollToken(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo("authorized");
            assertThat(response.getBody().getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().getUser().getEmail()).isEqualTo("test@example.com");
        }
    }
    
    @Nested
    @DisplayName("GET /api/auth/device/verify - ユーザーコード検証")
    class VerifyUserCodeTest {
        
        @Test
        @DisplayName("有効なユーザーコードの場合はvalidを返す")
        @SuppressWarnings("unchecked")
        void shouldReturnValidForValidUserCode() {
            DeviceAuthSession session = DeviceAuthSession.builder()
                    .clientId("mirel-cli")
                    .scope("api:read")
                    .build();
            
            when(deviceAuthService.validateUserCode("ABCD-EFGH"))
                    .thenReturn(Optional.of(session));
            
            ResponseEntity<?> response = controller.verifyUserCode("ABCD-EFGH");
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(true);
            assertThat(body.get("client_id")).isEqualTo("mirel-cli");
        }
        
        @Test
        @DisplayName("無効なユーザーコードの場合はvalidがfalseを返す")
        @SuppressWarnings("unchecked")
        void shouldReturnInvalidForInvalidUserCode() {
            when(deviceAuthService.validateUserCode("XXXX-XXXX"))
                    .thenReturn(Optional.empty());
            
            ResponseEntity<?> response = controller.verifyUserCode("XXXX-XXXX");
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
            assertThat(body.get("valid")).isEqualTo(false);
            assertThat(body.get("error")).isEqualTo("invalid_code");
        }
    }
    
    @Nested
    @DisplayName("POST /api/auth/device/authorize - ユーザーコード承認")
    class AuthorizeTest {
        
        @Test
        @DisplayName("認証済みユーザーが承認できる")
        @SuppressWarnings("unchecked")
        void shouldAuthorizeWhenAuthenticated() {
            User mockUser = new User();
            mockUser.setUserId("user-123");
            mockUser.setUsername("testuser");
            mockUser.setDisplayName("Test User");
            mockUser.setEmail("test@example.com");
            
            when(executionContext.isAuthenticated()).thenReturn(true);
            when(executionContext.getCurrentUser()).thenReturn(mockUser);
            when(deviceAuthService.authorize("ABCD-EFGH", "user-123", "Test User", "test@example.com"))
                    .thenReturn(true);
            
            DeviceAuthorizationRequest request = DeviceAuthorizationRequest.builder()
                    .userCode("ABCD-EFGH")
                    .build();
            
            ResponseEntity<?> response = controller.authorize(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
        }
        
        @Test
        @DisplayName("未認証ユーザーは401を返す")
        @SuppressWarnings("unchecked")
        void shouldReturn401WhenNotAuthenticated() {
            when(executionContext.isAuthenticated()).thenReturn(false);
            
            DeviceAuthorizationRequest request = DeviceAuthorizationRequest.builder()
                    .userCode("ABCD-EFGH")
                    .build();
            
            ResponseEntity<?> response = controller.authorize(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
            assertThat(body.get("error")).isEqualTo("unauthorized");
        }
    }
    
    @Nested
    @DisplayName("POST /api/auth/device/deny - ユーザーコード拒否")
    class DenyTest {
        
        @Test
        @DisplayName("認証済みユーザーが拒否できる")
        @SuppressWarnings("unchecked")
        void shouldDenyWhenAuthenticated() {
            when(executionContext.isAuthenticated()).thenReturn(true);
            when(deviceAuthService.deny("ABCD-EFGH")).thenReturn(true);
            
            DeviceAuthorizationRequest request = DeviceAuthorizationRequest.builder()
                    .userCode("ABCD-EFGH")
                    .build();
            
            ResponseEntity<?> response = controller.deny(request);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
        }
    }
}
