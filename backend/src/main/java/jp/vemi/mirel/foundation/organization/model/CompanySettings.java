/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

/**
 * 会社設定.
 * Root組織（type=COMPANY）に紐づく会社レベルの設定を管理する。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_company_settings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "period_code"}))
public class CompanySettings {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "period_code")
    private String periodCode;

    @Column(name = "fiscal_year_start")
    private Integer fiscalYearStart = 4; // 4月始まり

    @Column(name = "currency_code", length = 3)
    private String currencyCode = "JPY";

    @Column(name = "timezone", length = 50)
    private String timezone = "Asia/Tokyo";

    @Column(name = "locale", length = 10)
    private String locale = "ja_JP";

    /** バージョン */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** 削除フラグ */
    @Column(name = "delete_flag", columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column(name = "create_user_id")
    private String createUserId;

    /** 作成日 */
    @Column(name = "create_date")
    private Date createDate;

    /** 更新ユーザ */
    @Column(name = "update_user_id")
    private String updateUserId;

    /** 更新日 */
    @Column(name = "update_date")
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final CompanySettings entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        entity.updateDate = new Date();
    }
}
