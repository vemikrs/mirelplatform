/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC設定クラス
 * 
 * @author Hiroki Kurosawa
 * @version 3.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RequestLoggingInterceptor())
        .addPathPatterns("/**")
        .order(Ordered.HIGHEST_PRECEDENCE);
  }
}