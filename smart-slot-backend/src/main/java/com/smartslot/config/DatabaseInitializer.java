package com.smartslot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartslot.constant.UserRole;
import com.smartslot.entity.SysUser;
import com.smartslot.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据库自愈与多角色账户初始化加载器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final SysUserService sysUserService;

    @Override
    public void run(String... args) {
        initOperationLogTable();
        initRolesAndUsers();
    }

    private void initOperationLogTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS `sys_operation_log` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
                  `username` VARCHAR(64) DEFAULT NULL COMMENT '操作人用户名',
                  `role` VARCHAR(32) DEFAULT NULL COMMENT '操作人角色',
                  `module` VARCHAR(64) NOT NULL COMMENT '操作模块',
                  `operation` VARCHAR(128) NOT NULL COMMENT '具体操作描述',
                  `method` VARCHAR(128) NOT NULL COMMENT '请求方法类名与方法名',
                  `params` TEXT COMMENT '操作入参(敏感信息脱敏)',
                  `result` VARCHAR(255) COMMENT '执行结果: SUCCESS / ERROR',
                  `duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时(毫秒)',
                  `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作人客户端IP',
                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                  PRIMARY KEY (`id`),
                  KEY `idx_user_id` (`user_id`),
                  KEY `idx_module` (`module`),
                  KEY `idx_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作审计日志表';
            """;
            jdbcTemplate.execute(sql);
            log.info("数据库初始化: sys_operation_log 审计日志表检查就绪");
        } catch (Exception e) {
            log.warn("检查或创建 sys_operation_log 遇到异常 (如使用内存库可忽略): {}", e.getMessage());
        }
    }

    private void initRolesAndUsers() {
        try {
            // 预置 123456 兼容哈希
            String defaultPwd = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

            // 1. 场馆运营店长
            SysUser manager = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "manager"));
            if (manager == null) {
                sysUserService.save(SysUser.builder()
                        .username("manager")
                        .password(defaultPwd)
                        .nickname("场馆运营店长")
                        .phone("13800001111")
                        .role(UserRole.ROLE_MANAGER)
                        .balance(new BigDecimal("5000.00"))
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build());
                log.info("初始化 RBAC 角色账户: manager / 123456 (ROLE_MANAGER)");
            }

            // 2. 前台核销专员
            SysUser verifier = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "verifier"));
            if (verifier == null) {
                sysUserService.save(SysUser.builder()
                        .username("verifier")
                        .password(defaultPwd)
                        .nickname("前台核销专员")
                        .phone("13800002222")
                        .role(UserRole.ROLE_VERIFIER)
                        .balance(new BigDecimal("1000.00"))
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build());
                log.info("初始化 RBAC 角色账户: verifier / 123456 (ROLE_VERIFIER)");
            }
        } catch (Exception e) {
            log.warn("初始化 RBAC 默认用户异常 (若库不可写可忽略): {}", e.getMessage());
        }
    }
}
