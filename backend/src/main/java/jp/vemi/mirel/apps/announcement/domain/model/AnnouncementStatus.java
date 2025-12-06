package jp.vemi.mirel.apps.announcement.domain.model;

public enum AnnouncementStatus {
    DRAFT, // 下書き
    SCHEDULED, // 予約公開待ち
    PUBLISHED, // 公開中
    ARCHIVED // アーカイブ済み
}
