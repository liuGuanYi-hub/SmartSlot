package com.smartslot.aspect;

import com.smartslot.annotation.RequiresRoles;
import com.smartslot.common.BusinessException;
import com.smartslot.common.UserContext;
import com.smartslot.constant.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC 权限校验切面
 */
@Slf4j
@Aspect
@Component
@Order(1) // 优先于业务逻辑和审计日志切面执行
public class RoleCheckAspect {

    @Before("@annotation(requiresRoles) || @within(requiresRoles)")
    public void checkRole(JoinPoint joinPoint, RequiresRoles requiresRoles) {
        String currentRole = UserContext.getRole();
        if (currentRole == null) {
            throw new BusinessException(401, "请先登录后再进行此操作");
        }

        // 超级管理员默认具备最高访问特权
        if (UserRole.ROLE_ADMIN.equals(currentRole)) {
            return;
        }

        List<String> allowedRoles = Arrays.asList(requiresRoles.value());
        boolean hasAccess;
        if (requiresRoles.logical() == RequiresRoles.Logical.AND) {
            hasAccess = allowedRoles.contains(currentRole) && allowedRoles.size() == 1;
        } else {
            hasAccess = allowedRoles.contains(currentRole);
        }

        if (!hasAccess) {
            log.warn("RBAC 拦截非授权访问: user={}, currentRole={}, requiredRoles={}",
                    UserContext.get().getUsername(), currentRole, allowedRoles);
            throw new BusinessException(403, "权限不足：当前功能需要角色 " + allowedRoles);
        }
    }
}
