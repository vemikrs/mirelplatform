package jp.vemi.mirel.apps.schema.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicModelHeader;

@Repository
public interface SchDicModelHeaderRepository extends JpaRepository<SchDicModelHeader, String> {
}
