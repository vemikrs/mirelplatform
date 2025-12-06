package jp.vemi.mirel.apps.announcement.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jp.vemi.mirel.apps.announcement.domain.model.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mir_announcement_target")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementTarget {
    @Id
    private String id;

    @Column(name = "announcement_id")
    private String announcementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "target_id")
    private String targetId; // targetTypeに応じて tenant_id, role, user_id, org_id
}
