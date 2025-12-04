package jp.vemi.mirel.apps.announcement.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.announcement.domain.entity.AnnouncementTarget;

@Repository
public interface AnnouncementTargetRepository extends JpaRepository<AnnouncementTarget, String> {
    List<AnnouncementTarget> findByAnnouncementId(String announcementId);

    List<AnnouncementTarget> findByAnnouncementIdIn(List<String> announcementIds);

    void deleteByAnnouncementId(String announcementId);
}
