package jp.vemi.mirel.apps.studio.modeler.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stu_dic_code")
@EntityListeners(AuditingEntityListener.class)
public class StuCode {

    @EmbeddedId
    private PK pk;

    @Column(name = "group_text")
    private String groupText;

    @Column(name = "text")
    private String text;

    @Column(name = "sort")
    private Long sort;

    @Column(name = "delete_flag")
    private Boolean deleteFlag;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class PK implements Serializable {
        @Column(name = "group_id")
        private String groupId;

        @Column(name = "code")
        private String code;
    }
}
