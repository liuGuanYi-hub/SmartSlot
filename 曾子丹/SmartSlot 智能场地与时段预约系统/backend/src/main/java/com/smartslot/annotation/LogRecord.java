package com.smartslot.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解 (Enterprise Audit Trail)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRecord {

    /**
     * 功能模块名 (如: 场地管理、订单核销、系统鉴权)
     */
    String module() default "";

    /**
     * 具体操作描述 (如: 新增场地、前台扫码核销、时段加锁)
     */
    String operation() default "";
}
