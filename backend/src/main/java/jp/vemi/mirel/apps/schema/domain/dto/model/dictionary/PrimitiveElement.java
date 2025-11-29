package jp.vemi.mirel.apps.schema.domain.dto.model.dictionary;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrimitiveElement extends AbstractSchemaDictionaryTreeNode {
    private String modelId;
    private String fieldId;
    private String fieldName;
    private String widgetType;
    private String dataType;
    private Boolean isKey;
    private Boolean isRequired;
    private Boolean isHeader;
    private Long sort;
    private String relationCodeGroup;
    private String defaultValue;
    private String function;
    private Long displayWidth;
    private String format;
    private BigDecimal maxDigits;
    private String description;
    private String regexPattern;
    private Integer minLength;
    private Integer maxLength;
    private BigDecimal minValue;
    private BigDecimal maxValue;
}
