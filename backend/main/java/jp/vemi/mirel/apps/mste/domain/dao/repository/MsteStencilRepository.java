/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;

/**
 * {@link MsteStencil} のrepositoryです。
 */
@Repository
public interface MsteStencilRepository extends JpaRepository<MsteStencil, String> {


    public List<MsteStencil> findByStencilName(String stencilName);

    @Query(value = "from MsteStencil s where s.stencilCd like :stencilCd% and s.itemKind = :itemKind order by sort")
    public List<MsteStencil> findByStencilCd(@Param("stencilCd") String stencilCd, @Param("itemKind") String itemKind);
}
