package jp.vemi.mirel.apps.schema.domain.dto.model.dictionary;

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
public class HierarchicalTopNode extends AbstractSchemaDictionaryTreeNode {
    private String modelId;
}
