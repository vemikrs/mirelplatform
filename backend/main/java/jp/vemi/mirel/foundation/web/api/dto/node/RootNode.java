package jp.vemi.mirel.foundation.web.api.dto.node;


import jp.vemi.mirel.foundation.web.api.types.NodeType;

public class RootNode extends Node {

    @Override
    public NodeType getNodeType() {
        return NodeType.ROOT;
    }

    @Override
    public void addChild(Node node) {
        super.childs.add(node);
    }
}
