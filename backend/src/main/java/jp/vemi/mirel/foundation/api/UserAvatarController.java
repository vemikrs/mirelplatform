/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.api;

import jp.vemi.mirel.foundation.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Avatar API Controller
 * 
 * ユーザーアバター画像の取得エンドポイントを提供します。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserAvatarController {
    
    private final AvatarService avatarService;
    
    /**
     * ユーザーアバター画像を取得します。
     * 
     * @param userId ユーザーID
     * @return アバター画像（バイナリ）
     */
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable UUID userId) {
        byte[] avatarBytes = avatarService.getAvatar(userId);
        
        if (avatarBytes == null) {
            log.debug("Avatar not found for user: {}, returning 404", userId);
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // デフォルトJPEG
        headers.setContentLength(avatarBytes.length);
        headers.setCacheControl("public, max-age=3600"); // 1時間キャッシュ
        
        return new ResponseEntity<>(avatarBytes, headers, HttpStatus.OK);
    }
}
