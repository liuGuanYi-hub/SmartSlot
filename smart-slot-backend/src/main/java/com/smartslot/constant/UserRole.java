package com.smartslot.constant;

/**
 * RBAC 系统多角色常量定义
 */
public interface UserRole {
    /**
     * 系统超级管理员 (全权管理、大屏看板、数据导出、操作审计)
     */
    String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 场馆运营店长 (大屏看板、场地配置、排期排查、订单管理)
     */
    String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * 前台核销员 (仅核销入场与核销记录检索)
     */
    String ROLE_VERIFIER = "ROLE_VERIFIER";

    /**
     * 普通尊享会员 (选座预约、我的行程、票据凭证)
     */
    String ROLE_USER = "ROLE_USER";
}
