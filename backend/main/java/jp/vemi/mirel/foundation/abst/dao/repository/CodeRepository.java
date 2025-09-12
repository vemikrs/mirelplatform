/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.vemi.mirel.foundation.abst.dao.entity.Code;

/**
 * {@link Code} のrepositoryです。.<br/>
 */
public interface CodeRepository
    extends JpaRepository<Code, String>{

}
