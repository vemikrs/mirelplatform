/*
 * Copyright(c) 2015-2025 vemi/mirelplatform.
 */
package jp.vemi.mirel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableAutoConfiguration
@EntityScan(basePackages = { "jp.vemi.mirel", "jp.vemi.framework" })
@EnableJpaRepositories(basePackages = { "jp.vemi.mirel", "jp.vemi.framework" })
@ComponentScan(basePackages = {
        "jp.vemi.framework.util",
        "jp.vemi.framework.config",
        "jp.vemi.framework.security",
        "jp.vemi.framework.web",
        "jp.vemi.mirel",
        "jp.vemi.mirel.config",
        "jp.vemi.mirel.security"
})
public class MiplaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiplaApplication.class, args);
    }
}
