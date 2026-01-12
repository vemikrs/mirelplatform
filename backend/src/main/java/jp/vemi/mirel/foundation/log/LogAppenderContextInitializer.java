/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Logback Appender に Spring ApplicationContext を提供するためのコンポーネント。
 * <p>
 * Logback は Spring 管理外で初期化されるため、このコンポーネントを通じて
 * ApplicationContext を静的に設定します。
 * </p>
 */
@Component
public class LogAppenderContextInitializer implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // R2LogAppender.setApplicationContext(applicationContext); // R2LogAppender
        // 削除済みのため不要
        // OneFilePerExceptionAppender も ApplicationContext 不要化済み
    }
}
