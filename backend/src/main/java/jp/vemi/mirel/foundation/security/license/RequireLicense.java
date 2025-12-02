/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.security.license;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ライセンス要求アノテーション.
 * メソッドまたはクラスに付与して、特定のライセンスを要求する
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLicense {
    /**
     * アプリケーションID
     */
    String application();

    /**
     * 必要なライセンスティア（デフォルトはFREE）
     */
    LicenseTier tier() default LicenseTier.FREE;
}
