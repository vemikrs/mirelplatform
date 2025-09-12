package jp.vemi.mirel.foundation.web.api.dto.node;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

import jp.vemi.mirel.foundation.web.api.types.NodeType;

public abstract class Node {

    public List<Node> childs = new ArrayList<>();

    public abstract NodeType getNodeType();

    public abstract void addChild(Node node);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
