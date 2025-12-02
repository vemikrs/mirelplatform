/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelation;
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
@ContextConfiguration(classes = StuRelationRepositoryTest.Config.class)
class StuRelationRepositoryTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "jp.vemi.mirel.apps.studio.domain.dao.repository")
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "jp.vemi.mirel.apps.studio.domain.dao.entity")
    static class Config {
    }

    @Autowired
    private StuRelationRepository repository;

    @Test
    void testFindBySourceAndTargetModelId() {
        String sourceId = UUID.randomUUID().toString();
        String targetId = UUID.randomUUID().toString();

        StuRelation relation = new StuRelation();
        relation.setRelationId(UUID.randomUUID().toString());
        relation.setSourceModelId(sourceId);
        relation.setTargetModelId(targetId);
        relation.setRelationType("ONE_TO_MANY");
        repository.save(relation);

        List<StuRelation> bySource = repository.findBySourceModelId(sourceId);
        assertThat(bySource).hasSize(1);
        assertThat(bySource.get(0).getTargetModelId()).isEqualTo(targetId);

        List<StuRelation> byTarget = repository.findByTargetModelId(targetId);
        assertThat(byTarget).hasSize(1);
        assertThat(byTarget.get(0).getSourceModelId()).isEqualTo(sourceId);
    }
}
