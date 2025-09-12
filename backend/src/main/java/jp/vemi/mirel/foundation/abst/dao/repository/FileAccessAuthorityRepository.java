/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.vemi.mirel.foundation.abst.dao.entity.FileAccessAuthority;

public interface FileAccessAuthorityRepository
    extends JpaRepository<FileAccessAuthority, String>{

}