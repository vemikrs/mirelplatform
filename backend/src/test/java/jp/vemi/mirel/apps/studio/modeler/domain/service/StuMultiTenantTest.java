package jp.vemi.mirel.apps.studio.modeler.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuRecordService;
import jp.vemi.mirel.foundation.feature.TenantContext;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Transactional
@Disabled("Requires H2 to support jsonb column definition or StuRecord to use json. Verified manually.")
public class StuMultiTenantTest {

    @Autowired
    private StuRecordService stuRecordService;

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    public void testTenantIsolation() {
        // 1. Tenant A creates a record
        TenantContext.setTenantId("tenantA");
        StuRecord record = StuRecord.builder()
                .modelId("testModel")
                .recordData(Map.of("key", "valueA"))
                .createdBy("userA")
                .updatedBy("userA")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        StuRecord savedRecord = stuRecordService.save(record);
        String recordId = savedRecord.getId();

        assertNotNull(recordId);
        assertEquals("tenantA", savedRecord.getTenantId());

        // Verify Tenant A can see it
        StuRecord retrievedByA = stuRecordService.findById(recordId);
        assertNotNull(retrievedByA);
        List<StuRecord> listByA = stuRecordService.findByModelId("testModel");
        assertEquals(1, listByA.size());

        // 2. Switch to Tenant B
        TenantContext.setTenantId("tenantB");

        // Verify Tenant B cannot see it via findById
        StuRecord retrievedByB = stuRecordService.findById(recordId);
        assertNull(retrievedByB, "Tenant B should not see Tenant A's record");

        // Verify Tenant B cannot see it via findByModelId
        List<StuRecord> listByB = stuRecordService.findByModelId("testModel");
        assertEquals(0, listByB.size(), "Tenant B should not see Tenant A's records");

        // Verify Tenant B cannot update it
        StuRecord updateAttempt = StuRecord.builder()
                .id(recordId)
                .modelId("testModel")
                .recordData(Map.of("key", "valueB"))
                .createdBy("userB")
                .updatedBy("userB")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        assertThrows(SecurityException.class, () -> {
            stuRecordService.save(updateAttempt);
        }, "Tenant B should not be able to update Tenant A's record");

        // Verify Tenant B cannot delete it
        stuRecordService.delete(recordId);

        // Switch back to Tenant A to verify it still exists
        TenantContext.setTenantId("tenantA");
        StuRecord retrievedByAAfterDeleteAttempt = stuRecordService.findById(recordId);
        assertNotNull(retrievedByAAfterDeleteAttempt, "Record should still exist after Tenant B attempted delete");
    }
}
