package jp.vemi.mirel.apps.announcement.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementCategory;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementPriority;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mir_announcement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    @Id
    @Column(name = "announcement_id")
    private String announcementId;

    @Column(name = "tenant_id")
    private String tenantId; // null = システム全体向け

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // Markdown対応

    private String summary; // 一覧表示用概要

    @Enumerated(EnumType.STRING)
    private AnnouncementCategory category;

    @Enumerated(EnumType.STRING)
    private AnnouncementPriority priority;

    @Enumerated(EnumType.STRING)
    private AnnouncementStatus status;

    @Column(name = "publish_at")
    private Instant publishAt; // 公開日時（予約投稿）

    @Column(name = "expire_at")
    private Instant expireAt; // 有効期限

    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "requires_acknowledgment")
    @Builder.Default
    private Boolean requiresAcknowledgment = false; // 確認必須

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
