/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import jp.vemi.framework.util.DataSeeder;

@Configuration
@Order(20)
@DependsOn("dataSeeder")
public class DatabaseInitializer {

    @Bean
    @ConditionalOnProperty(name = "mirel.database.initialize", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner initializer() {
        return args -> {
            DataSeeder.initializeDefaultTenant();
            DataSeeder.initializeSaasTestData();
            DataSeeder.initializeSchemaSampleData();
        };
    }
}
