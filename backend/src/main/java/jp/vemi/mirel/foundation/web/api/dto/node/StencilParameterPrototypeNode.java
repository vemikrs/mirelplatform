/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.dto.node;

import jp.vemi.mirel.foundation.web.api.types.NodeType;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

@Builder
@ToString
public class StencilParameterPrototypeNode extends Node {

    @NonNull
    public String id;

    public String name;

    public String valueType;

    public String value;

    public String placeholder;

    public String note;

    public Integer sort;

    public Boolean noSend;

    public String postEvent;

    @Override
    public NodeType getNodeType() {
        return NodeType.ELEMENT;
    }

    @Override
    public void addChild(Node node) {
        throw new IllegalAccessError();
    }

}