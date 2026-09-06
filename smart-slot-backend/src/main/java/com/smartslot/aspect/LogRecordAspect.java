package com.smartslot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartslot.annotation.LogRecord;
import com.smartslot.common.UserContext;
import com.smartslot.entity.OperationLog;
import com.smartslot.service.OperationLogService;
import com.smartslot.util.DataMaskUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * AOP 操作审计日志切面
 */
@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class LogRecordAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logRecord)")
    public Object recordLog(ProceedingJoinPoint joinPoint, LogRecord logRecord) throws Throwable {
        long start = System.currentTimeMillis();
        String resultStatus = "SUCCESS";
        Throwable error = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            resultStatus = "ERROR: " + ex.getMessage();
            error = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                saveAuditLog(joinPoint, logRecord, duration, resultStatus);
            } catch (Exception e) {
                log.error("记录操作审计日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, LogRecord logRecord, long duration, String resultStatus) {
        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethod = className + "#" + methodName;

        // 提取与脱敏参数
        String paramsStr = "";
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 排除 request / response 参数
                StringBuilder sb = new StringBuilder();
                for (Object arg : args) {
                    if (arg instanceof HttpServletRequest) continue;
                    String json = objectMapper.writeValueAsString(arg);
                    // 敏感词简单安全替换
                    if (json.contains("password")) {
                        json = json.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"******\"");
                    }
                    sb.append(json).append("; ");
                }
                paramsStr = sb.length() > 500 ? sb.substring(0, 500) + "..." : sb.toString();
            }
        } catch (Exception ignored) {
            paramsStr = "[参数序列化略]";
        }

        UserContext.CurrentUserInfo currentUser = UserContext.get();
        OperationLog opLog = OperationLog.builder()
                .userId(currentUser != null ? currentUser.getUserId() : null)
                .username(currentUser != null ? currentUser.getUsername() : "ANONYMOUS")
                .role(currentUser != null ? currentUser.getRole() : "NONE")
                .module(logRecord.module())
                .operation(logRecord.operation())
                .method(fullMethod)
                .params(paramsStr)
                .result(resultStatus.length() > 250 ? resultStatus.substring(0, 250) : resultStatus)
                .durationMs(duration)
                .ip(getClientIp(request))
                .createTime(LocalDateTime.now())
                .build();

        operationLogService.save(opLog);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
