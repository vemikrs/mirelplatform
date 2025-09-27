/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * コード
 */
@Entity
@Table(name = "mir_code")
public class Code {

    /** PK */
    @EmbeddedId
    public PK pk;

    /** テキスト */
    public String text;

    /** ソート */
    public long sort;

    /**
     * PK
     */
    @Embeddable
    public static class PK implements Serializable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 4589724568729L;

        /** 会社コード */
        @Column
        public String companyCd;

        /** コード分類コード */
        @Column
        public String codeCategoryCd;

        /** オード */
        @Column
        public String code;
    }
}