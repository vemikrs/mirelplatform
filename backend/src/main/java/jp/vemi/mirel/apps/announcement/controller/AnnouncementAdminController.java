package jp.vemi.mirel.apps.announcement.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.apps.announcement.domain.entity.Announcement;
import jp.vemi.mirel.apps.announcement.domain.service.AnnouncementService;
import jp.vemi.mirel.apps.announcement.dto.AnnouncementSaveDto;
import jp.vemi.mirel.apps.announcement.dto.AnnouncementSearchCondition;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AnnouncementAdminController {

    private final AnnouncementService announcementService;
    private final ExecutionContext executionContext;

    @PostMapping("/search")
    public ResponseEntity<Page<Announcement>> search(
            @RequestBody AnnouncementSearchCondition condition,
            @PageableDefault(size = 20, sort = "updatedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        // TODO: Admin check
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(announcementService.search(condition, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Announcement announcement = announcementService.get(id);
        var targets = announcementService.getTargets(id);

        return ResponseEntity.ok(Map.of(
                "announcement", announcement,
                "targets", targets));
    }

    @PostMapping
    public ResponseEntity<Announcement> create(@RequestBody AnnouncementSaveDto dto) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String authorId = executionContext.getCurrentUserId();
        String tenantId = executionContext.getCurrentTenantId();

        return ResponseEntity.ok(announcementService.create(dto, authorId, tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcement> update(@PathVariable String id, @RequestBody AnnouncementSaveDto dto) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(announcementService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        announcementService.delete(id);
        return ResponseEntity.ok().build();
    }
}
