package com.smartslot.aspect;

import com.smartslot.annotation.Idempotent;
import com.smartslot.common.BusinessException;
import com.smartslot.service.IdempotentTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口幂等性切面
 * 在执行带有 @Idempotent 的控制器方法前，提取 Header 中的 Idempotent-Token 并原子核销
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IdempotentTokenService idempotentTokenService;

    @Before("@annotation(idempotent)")
    public void checkIdempotency(Idempotent idempotent) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String headerName = idempotent.headerName();
        String token = request.getHeader(headerName);

        // 也支持从 Query 参数中读取
        if (!StringUtils.hasText(token)) {
            token = request.getParameter(headerName);
        }

        if (!StringUtils.hasText(token)) {
            if (idempotent.required()) {
                log.warn("接口幂等性校验拦截: 缺少 Header [{}]", headerName);
                throw new BusinessException("缺少防重放凭证 (" + headerName + ")，请刷新页面后重试");
            }
            return;
        }

        boolean valid = idempotentTokenService.verifyAndConsume(token);
        if (!valid) {
            log.warn("接口幂等性拦截生效: Token [{}] 无效或已被重复使用", token);
            throw new BusinessException(idempotent.message());
        }

        log.info("接口幂等性校验通过并成功原子核销 Token: [{}]", token);
    }
}
