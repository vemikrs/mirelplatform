/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
// import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * コード分類
 */
@Entity
@Table(name = "mir_code_category")
public class CodeCategory {

    /** ファイル名 */
    @EmbeddedId
    public PK pk;

    /** コード分類名 */
    public String codeCategoryName;

    /**
     * PK
     */
    @Embeddable
    public static class PK implements Serializable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 5435L;

        /** 会社コード */
        @Column
        public String companyCd;

        /** コード分類コード */
        @Column
        public String codeCategoryCd;

    }

}
