/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Date;

/**
 * ユーザー所属情報.
 */
@Setter
@Getter
@Entity
@Table(name = "mir_user_organization")
public class UserOrganization {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "unit_id", nullable = false)
    private String unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type")
    private PositionType positionType; // PRIMARY, SECONDARY, TEMPORARY

    @Column(name = "job_title")
    private String jobTitle; // 部長、課長、担当

    @Column(name = "job_grade")
    private Integer jobGrade; // 職位等級（承認権限判定用）

    @Column(name = "is_manager", columnDefinition = "boolean default false")
    private Boolean isManager = false; // この組織の長か

    @Column(name = "can_approve", columnDefinition = "boolean default false")
    private Boolean canApprove = false; // 承認権限

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** バージョン */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** 削除フラグ */
    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column()
    private String createUserId;

    /** 作成日 */
    @Column()
    private Date createDate;

    /** 更新ユーザ */
    @Column()
    private String updateUserId;

    /** 更新日 */
    @Column()
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final UserOrganization entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
