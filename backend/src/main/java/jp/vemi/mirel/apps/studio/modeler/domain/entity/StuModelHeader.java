package jp.vemi.mirel.apps.studio.modeler.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
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
@Table(name = "stu_dic_model_header")
@EntityListeners(AuditingEntityListener.class)
public class StuModelHeader {

    @Id
    @Column(name = "model_id")
    private String modelId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_hidden")
    private Boolean isHidden;

    @Column(name = "model_type")
    private String modelType;

    @Column(name = "model_category")
    private String modelCategory;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "status")
    private String status;

    @Column(name = "form_settings", length = 2000)
    private String formSettings;

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
}
