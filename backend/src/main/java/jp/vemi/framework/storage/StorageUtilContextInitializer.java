/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jp.vemi.framework.util.StorageUtil;

/**
 * StorageUtil に ApplicationContext を注入するためのコンポーネント。
 * <p>
 * Spring Bean 管理外の StorageUtil が StorageService にアクセスできるようにします。
 * </p>
 */
@Component
public class StorageUtilContextInitializer implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StorageUtil.setApplicationContext(applicationContext);
    }
}
