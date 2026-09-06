-- ============================================================
-- SmartSlot 智能场地/时段预约系统 数据库初始化脚本
-- 兼容 MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS `smart_slot` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_slot`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '登录密码(BCrypt/MD5加盐)',
  `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' COMMENT '头像URL',
  `role` VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色: ROLE_USER-普通会员, ROLE_ADMIN-管理员',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 1000.00 COMMENT '虚拟账户余额(元)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- 2. 场地分类表 (venue_category)
-- ----------------------------
DROP TABLE IF EXISTS `venue_category`;
CREATE TABLE `venue_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '分类名称(如羽毛球、篮球、网球、会议室)',
  `icon` VARCHAR(64) DEFAULT 'Basketball' COMMENT 'Element Plus 图标名',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地分类表';

-- ----------------------------
-- 3. 场地基础信息表 (venue)
-- ----------------------------
DROP TABLE IF EXISTS `venue`;
CREATE TABLE `venue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '场地ID',
  `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
  `name` VARCHAR(64) NOT NULL COMMENT '场地名称(如 羽毛球1号馆-VIP)',
  `capacity` INT NOT NULL DEFAULT 4 COMMENT '容纳人数',
  `price_per_hour` DECIMAL(8,2) NOT NULL DEFAULT 50.00 COMMENT '时段基础单价(元/小时)',
  `cover_image` VARCHAR(255) DEFAULT '' COMMENT '封面图片URL',
  `facilities` VARCHAR(255) DEFAULT '空调,木质地胶,独立更衣室,免费WiFi' COMMENT '设施标签(逗号分隔)',
  `description` TEXT COMMENT '场地详细介绍',
  `open_time` VARCHAR(10) NOT NULL DEFAULT '09:00' COMMENT '每日开放时间',
  `close_time` VARCHAR(10) NOT NULL DEFAULT '22:00' COMMENT '每日闭馆时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常开放, 0-维护中',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地信息表';

-- ----------------------------
-- 4. 预约订单表 (booking_order)
-- ----------------------------
DROP TABLE IF EXISTS `booking_order`;
CREATE TABLE `booking_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单编号',
  `user_id` BIGINT NOT NULL COMMENT '预约用户ID',
  `venue_id` BIGINT NOT NULL COMMENT '预约场地ID',
  `book_date` DATE NOT NULL COMMENT '预约日期(如 2026-09-05)',
  `time_slot` VARCHAR(32) NOT NULL COMMENT '预约时段(如 09:00-10:00)',
  `total_amount` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额(元)',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态: 0-未支付, 1-已支付, 2-已退款',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付锁定中, 1-预约成功(待核销), 2-已完成(已核销), 3-已取消',
  `verify_code` VARCHAR(16) DEFAULT NULL COMMENT '6位专属核销码',
  `contact_name` VARCHAR(32) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系人电话',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `verify_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '待支付超时锁定截止时间(通常15分钟)',
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_venue_date` (`venue_id`, `book_date`),
  KEY `idx_verify_code` (`verify_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约订单表';

-- ----------------------------
-- 5. 订单评价表 (order_review)
-- ----------------------------
DROP TABLE IF EXISTS `order_review`;
CREATE TABLE `order_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `venue_id` BIGINT NOT NULL COMMENT '关联场地ID',
  `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
  `rating` INT NOT NULL DEFAULT 5 COMMENT '评分(1-5星)',
  `content` VARCHAR(500) NOT NULL COMMENT '评价内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_venue_id` (`venue_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约评价表';

-- ----------------------------
-- 6. 操作审计日志表 (sys_operation_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
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

-- ============================================================
-- 初始演示数据导入
-- ============================================================

-- 初始管理员 (admin / 123456)、店长 (manager / 123456)、核销员 (verifier / 123456) 与测试会员 (user / 123456)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `role`, `balance`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统超级管理员', '13800000000', 'ROLE_ADMIN', 9999.00, 1),
(2, 'user', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '羽球小旋风', '13912345678', 'ROLE_USER', 600.00, 1),
(3, 'manager', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '中心场馆店长', '13812345678', 'ROLE_MANAGER', 5000.00, 1),
(4, 'verifier', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '前台核销专员', '13898765432', 'ROLE_VERIFIER', 1000.00, 1),
(5, 'user1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '先锋运动会员', '13800003333', 'ROLE_USER', 1000.00, 1);

-- 初始场地分类
INSERT INTO `venue_category` (`id`, `name`, `icon`, `sort`, `status`) VALUES
(1, '羽毛球馆', 'Basketball', 1, 1),
(2, '网球中心', 'Trophy', 2, 1),
(3, '篮球全场/半场', 'Football', 3, 1),
(4, '智能多功能会议室', 'Monitor', 4, 1);

-- 初始场地数据
INSERT INTO `venue` (`id`, `category_id`, `name`, `capacity`, `price_per_hour`, `cover_image`, `facilities`, `description`, `open_time`, `close_time`, `status`) VALUES
(1, 1, '羽毛球 1 号场 (奥运专业地胶)', 4, 60.00, 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,空调恒温,独立休息凳,免费Wi-Fi', '国际比赛级标准场地，灯光柔和防眩晕，适合高水平切磋与日常健身。', '09:00', '22:00', 1),
(2, 1, '羽毛球 2 号场 (双打标准场)', 4, 50.00, 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,更衣室,免费热水', '标准训练场地，通风良好，性价比极高。', '09:00', '22:00', 1),
(3, 1, '羽毛球 3 号场 (进阶训练场)', 4, 50.00, 'https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,独立休息区', '适合团队包场或小团体训练，设施齐全。', '09:00', '22:00', 1),
(4, 2, '中心网球 1 号场 (红土体验)', 4, 120.00, 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60', '红土球场,夜间高亮度泛光灯,球童服务', '法网同级红土脚感，减震效果极佳，带给您极致网球享受。', '09:00', '22:00', 1),
(5, 3, '室内篮球半场 A (木地板)', 10, 80.00, 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60', '美职篮同款枫木地板,计分屏,淋浴间', '全馆配备专业新风系统与弹性龙骨木地板，保护膝盖。', '09:00', '22:00', 1),
(6, 4, '云端多媒体会议室 (20人)', 20, 150.00, 'https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=800&auto=format&fit=crop&q=60', '4K双屏投影,远程视频会议系统,茶歇区,电子白板', '高规格商务路演与研讨会空间，静音隔音，配备千兆专线。', '09:00', '22:00', 1);

-- 初始模拟预约订单 (带 6 位核销码)
INSERT INTO `booking_order` (`id`, `order_no`, `user_id`, `venue_id`, `book_date`, `time_slot`, `total_amount`, `pay_status`, `order_status`, `verify_code`, `contact_name`, `contact_phone`, `pay_time`, `create_time`) VALUES
(1, 'ORD20260905001', 2, 1, CURRENT_DATE(), '10:00-11:00', 60.00, 1, 1, '839201', '张小凡', '13912345678', NOW(), NOW()),
(2, 'ORD20260905002', 2, 1, CURRENT_DATE(), '14:00-15:00', 60.00, 1, 2, '519302', '张小凡', '13912345678', NOW(), NOW());

-- 初始评价
INSERT INTO `order_review` (`id`, `order_id`, `venue_id`, `user_id`, `rating`, `content`, `create_time`) VALUES
(1, 2, 1, 2, 5, '场地灯光非常舒服，完全不刺眼，地胶弹性很棒，下次还来！', NOW());

SET FOREIGN_KEY_CHECKS = 1;
