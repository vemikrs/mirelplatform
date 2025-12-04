/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = jp.vemi.mirel.MiplaApplication.class)
class StuModelHeaderLegacyRepositoryTest {

    @Autowired
    private StuModelHeaderLegacyRepository repository;

    @Test
    void saveAndFind() {
        StuModelHeaderLegacy header = new StuModelHeaderLegacy();
        header.setModelId("test-model");
        header.setModelName("Test Model");
        header.setStatus("DRAFT");
        header.setVersion(1);

        repository.save(header);

        StuModelHeaderLegacy found = repository.findById("test-model").orElse(null);
        assertNotNull(found);
        assertEquals("Test Model", found.getModelName());
    }

    @Test
    void findByStatus() {
        StuModelHeaderLegacy h1 = new StuModelHeaderLegacy();
        h1.setModelId("m1");
        h1.setModelName("M1");
        h1.setStatus("DRAFT");
        h1.setVersion(1);
        repository.save(h1);

        StuModelHeaderLegacy h2 = new StuModelHeaderLegacy();
        h2.setModelId("m2");
        h2.setModelName("M2");
        h2.setStatus("PUBLISHED");
        h2.setVersion(1);
        repository.save(h2);

        List<StuModelHeaderLegacy> drafts = repository.findByStatus("DRAFT");
        assertEquals(1, drafts.size());
        assertEquals("m1", drafts.get(0).getModelId());
    }
}
