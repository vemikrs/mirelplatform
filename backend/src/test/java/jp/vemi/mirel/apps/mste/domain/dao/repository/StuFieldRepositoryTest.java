/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dao.repository;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuField;
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
@ContextConfiguration(classes = StuFieldRepositoryTest.Config.class)
class StuFieldRepositoryTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "jp.vemi.mirel.apps.mste.domain.dao.repository")
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "jp.vemi.mirel.apps.mste.domain.dao.entity")
    static class Config {
    }

    @Autowired
    private StuFieldRepository repository;

    @Test
    void testFindByModelIdOrderBySortOrder() {
        String modelId = UUID.randomUUID().toString();

        StuField field1 = new StuField();
        field1.setFieldId(UUID.randomUUID().toString());
        field1.setModelId(modelId);
        field1.setFieldName("Field 1");
        field1.setFieldType("STRING");
        field1.setIsRequired(true);
        field1.setSortOrder(2);
        repository.save(field1);

        StuField field2 = new StuField();
        field2.setFieldId(UUID.randomUUID().toString());
        field2.setModelId(modelId);
        field2.setFieldName("Field 2");
        field2.setFieldType("NUMBER");
        field2.setIsRequired(false);
        field2.setSortOrder(1);
        repository.save(field2);

        List<StuField> fields = repository.findByModelIdOrderBySortOrder(modelId);
        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getFieldName()).isEqualTo("Field 2");
        assertThat(fields.get(1).getFieldName()).isEqualTo("Field 1");
    }
}
