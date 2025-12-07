package jp.vemi.mirel.apps.mira.infrastructure.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira AI サービスの実行時間を計測するAspect
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MiraMetricsAspect {

    private final MiraMetrics metrics;

    @Around("execution(* jp.vemi.mirel.apps.mira.domain.service..*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                metrics.recordServiceExecution(className, methodName, duration);
            } catch (Exception e) {
                log.warn("Failed to record metrics for {}.{}: {}", className, methodName, e.getMessage());
            }
        }
    }
}
