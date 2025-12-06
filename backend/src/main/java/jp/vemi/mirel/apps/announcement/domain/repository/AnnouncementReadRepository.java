package jp.vemi.mirel.apps.announcement.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.announcement.domain.entity.AnnouncementRead;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, String> {
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(String announcementId, String userId);

    void deleteByAnnouncementId(String announcementId);

    List<AnnouncementRead> findByUserId(String userId);
}
