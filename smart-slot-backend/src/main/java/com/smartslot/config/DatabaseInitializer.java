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
        initIotGateLogTable();
        initPaymentRecordTable();
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

    private void initIotGateLogTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS `iot_gate_log` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `gate_id` VARCHAR(64) NOT NULL COMMENT '闸机硬件ID',
                  `venue_id` BIGINT DEFAULT NULL COMMENT '场馆ID',
                  `venue_name` VARCHAR(64) DEFAULT NULL COMMENT '场馆名称',
                  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '预约单号',
                  `verify_code` VARCHAR(16) DEFAULT NULL COMMENT '核销码',
                  `action` VARCHAR(32) NOT NULL COMMENT '操作指令',
                  `protocol` VARCHAR(32) NOT NULL DEFAULT 'MQTT_QOS1' COMMENT '通信协议',
                  `topic` VARCHAR(128) DEFAULT NULL COMMENT 'MQTT主题',
                  `payload_json` TEXT COMMENT '报文JSON',
                  `status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '执行结果',
                  `duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '响应耗时',
                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
                  PRIMARY KEY (`id`),
                  KEY `idx_gate_id` (`gate_id`),
                  KEY `idx_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物联网门禁道闸指令流水表';
            """;
            jdbcTemplate.execute(sql);
            log.info("数据库初始化: iot_gate_log 智能道闸日志表检查就绪");
        } catch (Exception e) {
            log.warn("检查或创建 iot_gate_log 遇到异常: {}", e.getMessage());
        }
    }

    private void initPaymentRecordTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS `payment_record` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `trade_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
                  `order_no` VARCHAR(64) NOT NULL COMMENT '系统订单号',
                  `user_id` BIGINT NOT NULL COMMENT '用户ID',
                  `channel` VARCHAR(32) NOT NULL COMMENT '支付渠道(ALIPAY/WECHAT/BALANCE)',
                  `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额',
                  `pay_status` INT NOT NULL DEFAULT 0 COMMENT '支付状态(0-待支付,1-支付成功,2-支付失败,3-已退款)',
                  `gateway_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '网关交易凭证号',
                  `buyer_id` VARCHAR(64) DEFAULT NULL COMMENT '买家网关标识',
                  `sign_type` VARCHAR(32) DEFAULT 'RSA2' COMMENT '签名算法',
                  `notify_raw_data` TEXT COMMENT '网关异步通知原始报文',
                  `reconciled` INT NOT NULL DEFAULT 1 COMMENT '对账状态(1-已平账,0-未平账)',
                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
                  `notify_time` DATETIME DEFAULT NULL COMMENT '网关回调通知时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_trade_no` (`trade_no`),
                  KEY `idx_order_no` (`order_no`),
                  KEY `idx_channel` (`channel`),
                  KEY `idx_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关流水对账表';
            """;
            jdbcTemplate.execute(sql);
            log.info("数据库初始化: payment_record 支付网关流水与对账表检查就绪");
        } catch (Exception e) {
            log.warn("检查或创建 payment_record 遇到异常: {}", e.getMessage());
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
