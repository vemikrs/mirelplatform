package jp.vemi.mirel.apps.schema.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
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
import jakarta.persistence.OrderBy;
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
@Table(name = "sch_dic_model")
@EntityListeners(AuditingEntityListener.class)
public class SchDicModel {

    @EmbeddedId
    private PK pk;

    @Column(name = "is_key")
    private Boolean isKey;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "widget_type")
    private String widgetType;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "description")
    private String description;

    @Column(name = "sort")
    @OrderBy("ASC")
    private Long sort;

    @Column(name = "is_header")
    private Boolean isHeader;

    @Column(name = "display_width")
    private Long displayWidth;

    @Column(name = "format")
    private String format;

    @Column(name = "constraint_id")
    private String constraintId;

    @Column(name = "max_digits")
    private BigDecimal maxDigits;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "relation_code_group")
    private String relationCodeGroup;

    @Column(name = "function")
    private String function;

    // Validation attributes
    @Column(name = "regex_pattern", length = 500)
    private String regexPattern;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Optimistic locking
    @Version
    @Column(name = "version")
    private Integer version;

    // Audit columns
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
        @Column(name = "model_id")
        private String modelId;

        @Column(name = "field_id")
        private String fieldId;
    }
}
