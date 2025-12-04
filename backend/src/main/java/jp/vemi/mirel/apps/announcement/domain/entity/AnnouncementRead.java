package jp.vemi.mirel.apps.announcement.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mir_announcement_read", uniqueConstraints = @UniqueConstraint(columnNames = { "announcement_id",
        "user_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRead {
    @Id
    private String id;

    @Column(name = "announcement_id")
    private String announcementId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "read_at")
    private Instant readAt; // 閲覧日時

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt; // 確認（同意）日時
}
