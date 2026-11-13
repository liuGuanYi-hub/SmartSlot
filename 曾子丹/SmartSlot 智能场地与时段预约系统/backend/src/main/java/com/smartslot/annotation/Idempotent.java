package com.smartslot.annotation;

import java.lang.annotation.*;

/**
 * 接口幂等性防护注解
 * 客户端先获取 Token，提交业务请求时在 Header 中携带 Idempotent-Token
 * 服务端基于 Redis / 本地原子校验并消费，杜绝网络抖动或表单重复连击导致的重复下单与扣费
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交请求，系统正在处理中";

    /**
     * 请求头中的 Token 键名 (默认 Idempotent-Token)
     */
    String headerName() default "Idempotent-Token";

    /**
     * 是否强制要求必须携带 Token (默认 true)
     */
    boolean required() default true;

    /**
     * Token 有效期（秒）
     */
    int expireSeconds() default 300;
}
