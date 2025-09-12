package jp.vemi.mirel.foundation.web.api.dto.node;

import jp.vemi.mirel.foundation.web.api.types.NodeType;

public class ItemNode extends Node {

    /** name */
    public String name;

    /** 初期値 */
    public String defaultValue;

    /** value */
    public String value;

    @Override
    public NodeType getNodeType() {
        return NodeType.STRING;
    }
    // ステンシルのエレメントメンバー

    @Override
    public void addChild(Node node) {
        throw new IllegalAccessError();
    }
}
