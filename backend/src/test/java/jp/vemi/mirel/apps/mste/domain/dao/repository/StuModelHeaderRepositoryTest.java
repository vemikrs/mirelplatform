/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dao.repository;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuModelHeader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@ContextConfiguration(classes = StuModelHeaderRepositoryTest.Config.class)
class StuModelHeaderRepositoryTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "jp.vemi.mirel.apps.mste.domain.dao.repository")
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "jp.vemi.mirel.apps.mste.domain.dao.entity")
    static class Config {
    }

    @Autowired
    private StuModelHeaderRepository repository;

    @Test
    void testSaveAndFind() {
        StuModelHeader header = new StuModelHeader();
        header.setModelId(UUID.randomUUID().toString());
        header.setModelName("Test Model");
        header.setStatus("DRAFT");
        header.setVersion(1);

        repository.save(header);

        StuModelHeader found = repository.findById(header.getModelId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getModelName()).isEqualTo("Test Model");
    }

    @Test
    void testFindByStatus() {
        StuModelHeader header1 = new StuModelHeader();
        header1.setModelId(UUID.randomUUID().toString());
        header1.setModelName("Model 1");
        header1.setStatus("DRAFT");
        header1.setVersion(1);
        repository.save(header1);

        StuModelHeader header2 = new StuModelHeader();
        header2.setModelId(UUID.randomUUID().toString());
        header2.setModelName("Model 2");
        header2.setStatus("PUBLISHED");
        header2.setVersion(1);
        repository.save(header2);

        List<StuModelHeader> drafts = repository.findByStatus("DRAFT");
        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getModelName()).isEqualTo("Model 1");
    }
}
