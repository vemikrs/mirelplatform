package jp.vemi.mirel.apps.announcement.dto;

import java.util.List;

import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementCategory;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementSearchCondition {
    private String title;
    private List<AnnouncementCategory> categories;
    private List<AnnouncementStatus> statuses;
}
