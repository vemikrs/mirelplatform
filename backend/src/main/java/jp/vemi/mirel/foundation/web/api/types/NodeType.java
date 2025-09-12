/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.foundation.web.api.types;

/**
 * ノードの列挙です。 .<br/>
 */
public enum NodeType {
    /** ルート */
    ROOT(0),
    /** 要素 */
    ELEMENT(1),
    /** 文字列 */
    STRING(2),
    ;

    protected int typeCode;

    private NodeType(int type) {
        this.typeCode = type;
    }
}
