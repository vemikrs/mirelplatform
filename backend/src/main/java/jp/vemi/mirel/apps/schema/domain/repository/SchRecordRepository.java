package jp.vemi.mirel.apps.schema.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;

@Repository
public interface SchRecordRepository extends JpaRepository<SchRecord, String> {
    List<SchRecord> findBySchema(String schema);
}
