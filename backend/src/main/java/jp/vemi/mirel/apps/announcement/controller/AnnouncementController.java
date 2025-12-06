package jp.vemi.mirel.apps.announcement.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.apps.announcement.domain.entity.Announcement;
import jp.vemi.mirel.apps.announcement.domain.service.AnnouncementService;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ExecutionContext executionContext;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnnouncements() {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = executionContext.getCurrentUser();
        String tenantId = executionContext.getCurrentTenantId();
        List<String> roles = getRoles(user);

        List<Announcement> announcements = announcementService.getAnnouncementsForUser(user.getUserId(), tenantId,
                roles);
        List<String> readIds = announcementService.getReadAnnouncementIds(user.getUserId());

        return ResponseEntity.ok(Map.of(
                "items", announcements,
                "readIds", readIds));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = executionContext.getCurrentUser();
        String tenantId = executionContext.getCurrentTenantId();
        List<String> roles = getRoles(user);

        long count = announcementService.getUnreadCount(user.getUserId(), tenantId, roles);

        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = executionContext.getCurrentUserId();
        announcementService.markAsRead(id, userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/read")
    public ResponseEntity<Void> markAsUnread(@PathVariable String id) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = executionContext.getCurrentUserId();
        announcementService.markAsUnread(id, userId);

        return ResponseEntity.ok().build();
    }

    private List<String> getRoles(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return Arrays.asList(user.getRoles().split(","));
    }
}
