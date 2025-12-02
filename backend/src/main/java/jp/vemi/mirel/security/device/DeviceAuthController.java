/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.security.device.dto.DeviceAuthorizationRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeResponse;
import jp.vemi.mirel.security.device.dto.DeviceTokenRequest;
import jp.vemi.mirel.security.device.dto.DeviceTokenResponse;

import java.util.Map;

/**
 * デバイス認証APIコントローラ。
 * OAuth2 Device Authorization Grant (RFC 8628) のエンドポイントを提供。
 */
@RestController
@RequestMapping("/api/auth/device")
public class DeviceAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthController.class);
    
    @Autowired
    private DeviceAuthService deviceAuthService;
    
    @Autowired
    private ExecutionContext executionContext;
    
    /**
     * デバイスコード発行エンドポイント。
     * CLIからの認証開始時に呼び出され、device_codeとuser_codeを発行します。
     * 
     * @param request デバイスコード発行リクエスト
     * @return デバイスコードレスポンス
     */
    @PostMapping("/code")
    public ResponseEntity<DeviceCodeResponse> requestDeviceCode(@RequestBody DeviceCodeRequest request) {
        logger.info("Device code request received: clientId={}, scope={}", 
                request.getClientId(), request.getScope());
        
        // バリデーション
        if (request.getClientId() == null || request.getClientId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        DeviceCodeResponse response = deviceAuthService.createDeviceCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * トークンポーリングエンドポイント。
     * CLIが定期的にポーリングして認証完了を確認します。
     * 
     * @param request トークンリクエスト
     * @return トークンレスポンス
     */
    @PostMapping("/token")
    public ResponseEntity<DeviceTokenResponse> pollToken(@RequestBody DeviceTokenRequest request) {
        logger.debug("Token poll request: clientId={}, deviceCode={}", 
                request.getClientId(), request.getDeviceCode());
        
        // バリデーション
        if (request.getClientId() == null || request.getDeviceCode() == null) {
            return ResponseEntity.badRequest()
                    .body(DeviceTokenResponse.builder()
                            .error("invalid_request")
                            .errorDescription("Missing required parameters")
                            .build());
        }
        
        DeviceTokenResponse response = deviceAuthService.pollToken(request.getDeviceCode(), request.getClientId());
        
        // レート制限エラーの場合は429を返す
        if ("slow_down".equals(response.getError())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ユーザーコード検証エンドポイント。
     * ブラウザ認証ページでユーザーコードの有効性を確認します。
     * 
     * @param userCode ユーザーコード
     * @return 検証結果
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUserCode(@RequestParam("code") String userCode) {
        logger.debug("Verify user code: {}", userCode);
        
        return deviceAuthService.validateUserCode(userCode)
                .map(session -> ResponseEntity.ok(Map.<String, Object>of(
                        "valid", true,
                        "client_id", session.getClientId(),
                        "scope", session.getScope() != null ? session.getScope() : ""
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "valid", false,
                        "error", "invalid_code",
                        "error_description", "The user code is invalid or expired"
                )));
    }
    
    /**
     * ユーザーコード承認エンドポイント。
     * 認証済みユーザーがブラウザで承認を行います。
     * 
     * @param request 承認リクエスト
     * @return 処理結果
     */
    @PostMapping("/authorize")
    public ResponseEntity<Map<String, Object>> authorize(@RequestBody DeviceAuthorizationRequest request) {
        logger.info("Authorize request for user code: {}", request.getUserCode());
        
        // 認証チェック
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized", "error_description", "User not authenticated"));
        }
        
        var user = executionContext.getCurrentUser();
        boolean success = deviceAuthService.authorize(
                request.getUserCode(),
                user.getUserId(),
                user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                user.getEmail()
        );
        
        if (success) {
            logger.info("User code authorized by user: {}", user.getUserId());
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "invalid_code",
                            "error_description", "The user code is invalid or expired"
                    ));
        }
    }
    
    /**
     * ユーザーコード拒否エンドポイント。
     * 認証済みユーザーがブラウザで拒否を行います。
     * 
     * @param request 拒否リクエスト
     * @return 処理結果
     */
    @PostMapping("/deny")
    public ResponseEntity<Map<String, Object>> deny(@RequestBody DeviceAuthorizationRequest request) {
        logger.info("Deny request for user code: {}", request.getUserCode());
        
        // 認証チェック
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized", "error_description", "User not authenticated"));
        }
        
        boolean success = deviceAuthService.deny(request.getUserCode());
        
        if (success) {
            logger.info("User code denied");
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "invalid_code",
                            "error_description", "The user code is invalid or expired"
                    ));
        }
    }
}
