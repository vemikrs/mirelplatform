/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.security.license;

import jp.vemi.mirel.foundation.context.ExecutionContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * ライセンスチェックAspect.
 * @RequireLicenseアノテーションが付与されたメソッドの実行前にライセンスをチェック
 */
@Aspect
@Component
public class LicenseCheckAspect {

    private static final Logger logger = LoggerFactory.getLogger(LicenseCheckAspect.class);

    @Autowired
    private ExecutionContext executionContext;

    @Before("@annotation(jp.vemi.mirel.foundation.security.license.RequireLicense) || " +
            "@within(jp.vemi.mirel.foundation.security.license.RequireLicense)")
    public void checkLicense(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // メソッドレベルのアノテーションを優先
        RequireLicense requireLicense = method.getAnnotation(RequireLicense.class);

        // メソッドにない場合はクラスレベルのアノテーションをチェック
        if (requireLicense == null) {
            requireLicense = joinPoint.getTarget().getClass().getAnnotation(RequireLicense.class);
        }

        if (requireLicense != null) {
            String applicationId = requireLicense.application();
            var tier = requireLicense.tier();

            logger.debug("Checking license: application={}, tier={}, user={}, tenant={}",
                applicationId, tier, 
                executionContext.getCurrentUserId(),
                executionContext.getCurrentTenantId());

            if (!executionContext.hasLicense(applicationId, tier)) {
                logger.warn("License check failed: application={}, tier={}, user={}, tenant={}",
                    applicationId, tier,
                    executionContext.getCurrentUserId(),
                    executionContext.getCurrentTenantId());
                throw new LicenseRequiredException(applicationId, tier);
            }

            logger.debug("License check passed: application={}, tier={}", applicationId, tier);
        }
    }
}
