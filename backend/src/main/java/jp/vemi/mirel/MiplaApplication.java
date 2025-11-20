/*
 * Copyright(c) 2015-2025 vemi/mirelplatform.
 */
package jp.vemi.mirel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EntityScan(basePackages = { "jp.vemi.mirel", "jp.vemi.framework" }) // 要整理
@EnableJpaRepositories(basePackages = { "jp.vemi.mirel", "jp.vemi.framework" }) // 要整理
@ComponentScan(basePackages = {
        "jp.vemi.framework.util", // 要整理
        "jp.vemi.framework.config", // 要整理
        "jp.vemi.framework.security", // 要整理
        "jp.vemi.framework.web", // 要整理
        "jp.vemi.mirel",
        "jp.vemi.mirel.config",
        "jp.vemi.mirel.foundation.web.api",
        "jp.vemi.mirel.security"
})
public class MiplaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiplaApplication.class, args);
    }
}
