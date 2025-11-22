/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import jp.vemi.framework.util.DatabaseUtil;

@Configuration
@Order(20)
@DependsOn("databaseUtil")
public class DatabaseInitializer {

  @Bean
  public ApplicationRunner initializer() {
    return args -> {
      DatabaseUtil.initializeDefaultTenant();
      DatabaseUtil.initializeSaasTestData();
    };
  }
}
