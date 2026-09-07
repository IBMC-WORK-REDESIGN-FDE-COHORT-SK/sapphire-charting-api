package com.health.charting.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * AOP aspect that logs structured audit events for temperature metric accesses.
 *
 * <p>Pointcut targets:
 * <ul>
 *   <li>{@code MetricReadingController.getReadings(..)} — only when the {@code metric}
 *       path variable equals {@code "temperature"}.</li>
 *   <li>All methods in {@code TemperatureReadingController} (dedicated temperature controller).</li>
 * </ul>
 *
 * <p>Each audit log entry is a single-line JSON object emitted at INFO level via SLF4J,
 * compatible with structured log aggregation pipelines.
 *
 * <p>FR-022: All reads and writes of temperature data MUST produce an audit trail.
 */
@Slf4j
@Aspect
@Component
public class TemperatureAuditAspect {

    // -----------------------------------------------------------------------
    // Pointcuts
    // -----------------------------------------------------------------------

    /**
     * Matches the generic {@code getReadings} endpoint in MetricReadingController.
     * The temperature-specific filter is applied in the advice body.
     */
    @Pointcut("execution(* com.health.charting.controller.MetricReadingController.getReadings(..))")
    public void metricReadingControllerGetReadings() {}

    /**
     * Matches every public method in the dedicated TemperatureReadingController.
     */
    @Pointcut("execution(* com.health.charting.controller.TemperatureReadingController.*(..))")
    public void temperatureReadingControllerAllMethods() {}

    // -----------------------------------------------------------------------
    // Advice
    // -----------------------------------------------------------------------

    /**
     * Emits a structured audit log after successful return from
     * {@code MetricReadingController.getReadings} when the metric is "temperature".
     *
     * <p>The {@code metric} argument is the first parameter of {@code getReadings}.
     */
    @AfterReturning(
            pointcut = "metricReadingControllerGetReadings()",
            returning = "result"
    )
    public void auditMetricReadings(JoinPoint joinPoint, Object result) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return;
        }
        String metric = String.valueOf(args[0]);
        if (!"temperature".equalsIgnoreCase(metric)) {
            return; // Only audit temperature requests from the generic controller
        }

        // Extract userId from the second argument (requestParam "userId")
        String affectedUserId = args.length > 1 ? String.valueOf(args[1]) : "unknown";
        emitAuditLog("READ", affectedUserId);
    }

    /**
     * Emits a structured audit log after successful return from any method
     * in {@code TemperatureReadingController}.
     */
    @AfterReturning(
            pointcut = "temperatureReadingControllerAllMethods()",
            returning = "result"
    )
    public void auditTemperatureReadingController(JoinPoint joinPoint, Object result) {
        Object[] args = joinPoint.getArgs();
        // userId is the first @RequestParam across all TemperatureReadingController methods
        String affectedUserId = args.length > 0 ? String.valueOf(args[0]) : "unknown";
        emitAuditLog("READ", affectedUserId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void emitAuditLog(String operation, String affectedUserId) {
        String accessorId = resolveAccessorId();
        // Structured JSON audit entry — single line for log aggregation
        log.info(
                "{\"audit_event\":\"temperature.metric.access\","
                + "\"accessor_id\":\"{}\","
                + "\"operation\":\"{}\","
                + "\"affected_user_id\":\"{}\","
                + "\"timestamp\":\"{}\"}",
                accessorId,
                operation,
                affectedUserId,
                Instant.now()
        );
    }

    private String resolveAccessorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "anonymous";
        }
        return auth.getName();
    }
}

// Made with Bob
