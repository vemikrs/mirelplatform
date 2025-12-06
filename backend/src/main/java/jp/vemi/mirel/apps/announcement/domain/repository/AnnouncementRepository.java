package jp.vemi.mirel.apps.announcement.domain.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.announcement.domain.entity.Announcement;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementStatus;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface AnnouncementRepository
        extends JpaRepository<Announcement, String>, JpaSpecificationExecutor<Announcement> {

    // 公開中のお知らせを取得（期限切れでないもの）
    @Query("SELECT a FROM Announcement a WHERE a.status = :status AND a.publishAt <= :now AND (a.expireAt IS NULL OR a.expireAt > :now) ORDER BY a.isPinned DESC, a.publishAt DESC")
    List<Announcement> findPublishedAnnouncements(@Param("status") AnnouncementStatus status,
            @Param("now") Instant now);
}
