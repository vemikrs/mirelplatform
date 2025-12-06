package jp.vemi.mirel.apps.announcement.dto;

import java.time.Instant;
import java.util.List;

import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementCategory;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementPriority;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementStatus;
import jp.vemi.mirel.apps.announcement.domain.model.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementSaveDto {
    private String title;
    private String content;
    private String summary;
    private AnnouncementCategory category;
    private AnnouncementPriority priority;
    private AnnouncementStatus status;
    private Instant publishAt;
    private Instant expireAt;
    private Boolean isPinned;
    private Boolean requiresAcknowledgment;
    private List<TargetDto> targets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetDto {
        private TargetType targetType;
        private String targetId;
    }
}
