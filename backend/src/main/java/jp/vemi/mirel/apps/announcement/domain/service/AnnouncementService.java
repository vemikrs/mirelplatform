package jp.vemi.mirel.apps.announcement.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.announcement.domain.entity.Announcement;
import jp.vemi.mirel.apps.announcement.domain.entity.AnnouncementRead;
import jp.vemi.mirel.apps.announcement.domain.entity.AnnouncementTarget;
import jp.vemi.mirel.apps.announcement.domain.model.AnnouncementStatus;
import jp.vemi.mirel.apps.announcement.domain.repository.AnnouncementReadRepository;
import jp.vemi.mirel.apps.announcement.domain.repository.AnnouncementRepository;
import jp.vemi.mirel.apps.announcement.domain.repository.AnnouncementTargetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementTargetRepository targetRepository;
    private final AnnouncementReadRepository readRepository;

    /**
     * ユーザー向けのお知らせ一覧を取得
     * 
     * @param userId
     *            現在のユーザーID
     * @param tenantId
     *            現在のテナントID
     * @param roles
     *            ユーザーのロールリスト
     * @return お知らせリスト
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsForUser(String userId, String tenantId, List<String> roles) {
        Instant now = Instant.now();

        // 1. 公開中のお知らせを全件取得
        List<Announcement> published = announcementRepository.findPublishedAnnouncements(AnnouncementStatus.PUBLISHED,
                now);

        if (published.isEmpty()) {
            return List.of();
        }

        List<String> announcementIds = published.stream().map(Announcement::getAnnouncementId).toList();

        // 2. 配信対象を取得
        List<AnnouncementTarget> targets = targetRepository.findByAnnouncementIdIn(announcementIds);
        Map<String, List<AnnouncementTarget>> targetsByAnnouncement = targets.stream()
                .collect(Collectors.groupingBy(AnnouncementTarget::getAnnouncementId));

        // 3. フィルタリング
        return published.stream()
                .filter(announcement -> isTargetUser(announcement,
                        targetsByAnnouncement.get(announcement.getAnnouncementId()), userId, tenantId, roles))
                .collect(Collectors.toList());
    }

    /**
     * ユーザーが配信対象に含まれるか判定
     */
    private boolean isTargetUser(Announcement announcement, List<AnnouncementTarget> targets, String userId,
            String tenantId, List<String> roles) {
        // ターゲット設定がない場合は誰にも表示しない（あるいは全員？今回は明示的なターゲット設定を必須とする運用を想定するが、ALLがない場合は表示しない実装にする）
        // ただし、CSVデータなどでターゲット設定が漏れる可能性もあるため、ターゲットテーブルにレコードがない場合は「システム全体(ALL)」とみなす実装も考えられるが、
        // ここでは厳密にチェックする。

        if (targets == null || targets.isEmpty()) {
            // ターゲット指定がない場合、tenantIdが指定されていればそのテナントのみ、なければ全員（後方互換あるいは簡易設定）
            // 今回の設計では TargetType.ALL を明示的に使う方針だが、念のため。
            if (announcement.getTenantId() != null) {
                return tenantId != null && announcement.getTenantId().equals(tenantId);
            }
            return true; // デフォルト全員
        }

        for (AnnouncementTarget target : targets) {
            switch (target.getTargetType()) {
                case ALL:
                    return true;
                case TENANT:
                    if (tenantId != null && target.getTargetId().equals(tenantId)) {
                        return true;
                    }
                    break;
                case USER:
                    if (target.getTargetId().equals(userId)) {
                        return true;
                    }
                    break;
                case ROLE:
                    if (roles != null && roles.contains(target.getTargetId())) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * 未読件数を取得
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId, String tenantId, List<String> roles) {
        List<Announcement> announcements = getAnnouncementsForUser(userId, tenantId, roles);
        List<AnnouncementRead> reads = readRepository.findByUserId(userId);
        List<String> readIds = reads.stream().map(AnnouncementRead::getAnnouncementId).toList();

        return announcements.stream()
                .filter(a -> !readIds.contains(a.getAnnouncementId()))
                .count();
    }

    /**
     * 既読にする
     */
    @Transactional
    public void markAsRead(String announcementId, String userId) {
        if (readRepository.findByAnnouncementIdAndUserId(announcementId, userId).isPresent()) {
            return;
        }

        AnnouncementRead read = AnnouncementRead.builder()
                .id(UUID.randomUUID().toString())
                .announcementId(announcementId)
                .userId(userId)
                .readAt(Instant.now())
                .build();

        readRepository.save(read);
    }

    /**
     * 未読に戻す
     */
    @Transactional
    public void markAsUnread(String announcementId, String userId) {
        readRepository.findByAnnouncementIdAndUserId(announcementId, userId)
                .ifPresent(readRepository::delete);
    }

    /**
     * 既読状態を取得
     */
    @Transactional(readOnly = true)
    public List<String> getReadAnnouncementIds(String userId) {
        return readRepository.findByUserId(userId).stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toList());
    }

    /**
     * お知らせを検索
     */
    @Transactional(readOnly = true)
    public Page<Announcement> search(
            jp.vemi.mirel.apps.announcement.dto.AnnouncementSearchCondition condition,
            Pageable pageable) {

        return announcementRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condition.getTitle() != null && !condition.getTitle().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + condition.getTitle() + "%"));
            }

            if (condition.getCategories() != null && !condition.getCategories().isEmpty()) {
                predicates.add(root.get("category").in(condition.getCategories()));
            }

            if (condition.getStatuses() != null && !condition.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(condition.getStatuses()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * お知らせを作成
     */
    @Transactional
    public Announcement create(jp.vemi.mirel.apps.announcement.dto.AnnouncementSaveDto dto, String authorId,
            String tenantId) {
        Announcement announcement = new Announcement();
        announcement.setAnnouncementId(UUID.randomUUID().toString());
        announcement.setTenantId(tenantId);
        announcement.setAuthorId(authorId);
        announcement.setCreatedAt(Instant.now());

        mapDtoToEntity(dto, announcement);

        announcementRepository.save(announcement);
        saveTargets(announcement.getAnnouncementId(), dto.getTargets());

        return announcement;
    }

    /**
     * お知らせを更新
     */
    @Transactional
    public Announcement update(String announcementId, jp.vemi.mirel.apps.announcement.dto.AnnouncementSaveDto dto) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + announcementId));

        mapDtoToEntity(dto, announcement);
        announcement.setUpdatedAt(Instant.now());

        announcementRepository.save(announcement);

        // ターゲットの再作成
        targetRepository.deleteByAnnouncementId(announcementId);
        saveTargets(announcementId, dto.getTargets());

        return announcement;
    }

    /**
     * お知らせを削除
     */
    @Transactional
    public void delete(String announcementId) {
        targetRepository.deleteByAnnouncementId(announcementId);
        readRepository.deleteByAnnouncementId(announcementId);
        announcementRepository.deleteById(announcementId);
    }

    /**
     * お知らせを取得
     */
    @Transactional(readOnly = true)
    public Announcement get(String announcementId) {
        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + announcementId));
    }

    /**
     * お知らせのターゲットを取得
     */
    @Transactional(readOnly = true)
    public List<AnnouncementTarget> getTargets(String announcementId) {
        return targetRepository.findByAnnouncementIdIn(List.of(announcementId));
    }

    private void mapDtoToEntity(jp.vemi.mirel.apps.announcement.dto.AnnouncementSaveDto dto,
            Announcement announcement) {
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setSummary(dto.getSummary());
        announcement.setCategory(dto.getCategory());
        announcement.setPriority(dto.getPriority());
        announcement.setStatus(dto.getStatus());
        announcement.setPublishAt(dto.getPublishAt());
        announcement.setExpireAt(dto.getExpireAt());
        announcement.setIsPinned(dto.getIsPinned());
        announcement.setRequiresAcknowledgment(dto.getRequiresAcknowledgment());
    }

    private void saveTargets(String announcementId,
            List<jp.vemi.mirel.apps.announcement.dto.AnnouncementSaveDto.TargetDto> targetDtos) {
        if (targetDtos == null || targetDtos.isEmpty()) {
            return;
        }

        List<AnnouncementTarget> targets = targetDtos.stream().map(t -> {
            AnnouncementTarget target = new AnnouncementTarget();
            target.setId(UUID.randomUUID().toString());
            target.setAnnouncementId(announcementId);
            target.setTargetType(t.getTargetType());
            target.setTargetId(t.getTargetId());
            return target;
        }).collect(Collectors.toList());

        targetRepository.saveAll(targets);
    }
}
