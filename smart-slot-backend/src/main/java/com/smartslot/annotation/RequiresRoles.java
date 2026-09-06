package com.smartslot.annotation;

import java.lang.annotation.*;

/**
 * RBAC 细粒度接口角色访问控制注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRoles {

    /**
     * 允许访问的角色清单
     */
    String[] value();

    /**
     * 多角色逻辑判定规则：OR (满足其一即可) / AND (必须全部满足)
     */
    Logical logical() default Logical.OR;

    enum Logical {
        AND, OR
    }
}
