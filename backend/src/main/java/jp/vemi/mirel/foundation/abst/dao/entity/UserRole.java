/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "mir_user_role")
@IdClass(UserRole.UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "role_id")
    private String roleId;

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

    public static void setDefault(final UserRole entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }

    @Getter
    @Setter
    public static class UserRoleId implements Serializable {
        private String userId;
        private String roleId;

        // デフォルトコンストラクタ
        public UserRoleId() {}

        // パラメータ付きコンストラクタ
        public UserRoleId(String userId, String roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        // equalsとhashCodeのオーバーライド
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserRoleId that = (UserRoleId) o;

            if (!userId.equals(that.userId)) return false;
            return roleId.equals(that.roleId);
        }

        @Override
        public int hashCode() {
            int result = userId.hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }
}
