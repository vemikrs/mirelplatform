package jp.vemi.mirel.apps.schema.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicCode;

@Repository
public interface SchDicCodeRepository extends JpaRepository<SchDicCode, SchDicCode.PK> {
    List<SchDicCode> findByPkGroupId(String groupId);
}
