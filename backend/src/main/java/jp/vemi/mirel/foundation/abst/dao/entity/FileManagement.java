/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.DynamicUpdate;

import lombok.Getter;
import lombok.Setter;

/**
 * ファイル管理
 * @DynamicUpdate: 変更されたフィールドのみUPDATE文に含める
 * H2環境でのoptimistic locking競合を削減
 */
@Setter
@Getter
@Entity
@Table(name = "mir_file_management")
@DynamicUpdate
public class FileManagement {

    /** ファイルID */
    @Id
    @Column()
    public String fileId;

    /** 実ファイル名 */
    @Column(name = "real_file_name")
    public String realFileName;

    /** コンテンツタイプ */
    @Column(name = "content_type")
    public String contentType;

    /** ファイル名 */
    @Column()
    public String fileName;
    
    /** ファイルパス */
    @Column()
    public String filePath;
 
    /** 期限日 */
    @Column(name = "expire_date")
    public Date expireDate;

    /** 削除日 */
    @Column(name = "delete_date")
    public Date deleteDate;

    /** アップロード者ID */
    @Column(name = "uploader_user_id")
    public String uploaderUserId;

    /** バージョン（楽観ロック用） */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    public Long version;

    /** 削除フラグ */
    @Column(columnDefinition = "boolean default false")
    public Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column()
    public String createUserId;

    /** 作成日 */
    @Column()
    public Date createDate;

    /** 更新ユーザ */
    @Column()
    public String updateUserId;

    /** 更新日 */
    @Column()
    public Date updateDate;

    @PrePersist
    public void onPrePersist() {
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final FileManagement entity) {

        if(null == entity.createDate) {
            entity.createDate = new Date();
        }

        if(null == entity.updateDate) {
            entity.updateDate = new Date();
        }
        
        // @Versionはnullのままにしてhiberrateに自動初期化させる
        // 手動でversion設定するとHibernateが既存エンティティと誤認する
    }
}
