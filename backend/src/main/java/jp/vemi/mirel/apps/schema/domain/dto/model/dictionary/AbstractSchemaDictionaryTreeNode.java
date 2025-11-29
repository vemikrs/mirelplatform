package jp.vemi.mirel.apps.schema.domain.dto.model.dictionary;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractSchemaDictionaryTreeNode {
    private String nodeType;
    private List<AbstractSchemaDictionaryTreeNode> childs;
}
