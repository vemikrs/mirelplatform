package jp.vemi.mirel.apps.studio.modeler.domain.dto.model.dictionary;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractStuDictionaryTreeNode {
    private String nodeType;
    private List<AbstractStuDictionaryTreeNode> childs;
}
