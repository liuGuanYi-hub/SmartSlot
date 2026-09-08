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
  `total_amount` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '订单原价金额(元)',
  `coupon_id` BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
  `discount_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  `actual_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '券后实付金额',
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
  `rating` INT NOT NULL DEFAULT 5 COMMENT '综合评分(1-5星)',
  `env_rating` INT NOT NULL DEFAULT 5 COMMENT '环境评分',
  `facility_rating` INT NOT NULL DEFAULT 5 COMMENT '设施评分',
  `service_rating` INT NOT NULL DEFAULT 5 COMMENT '服务评分',
  `tags` VARCHAR(255) DEFAULT '' COMMENT '评价标签',
  `content` TEXT NOT NULL COMMENT '评价内容',
  `images` TEXT DEFAULT NULL COMMENT '实拍晒图URL集合',
  `merchant_reply` VARCHAR(500) DEFAULT NULL COMMENT '商家回复',
  `likes` INT NOT NULL DEFAULT 0 COMMENT '有用/点赞数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
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

-- 初始场地分类 (10 大综合体育运动分类)
INSERT INTO `venue_category` (`id`, `name`, `icon`, `sort`, `status`) VALUES
(1, '羽毛球馆', 'Basketball', 1, 1),
(2, '网球中心', 'Trophy', 2, 1),
(3, '篮球全场/半场', 'Football', 3, 1),
(4, '恒温游泳水上馆', 'Watermelon', 4, 1),
(5, '乒乓球中心', 'Medal', 5, 1),
(6, '台球/斯诺克俱乐部', 'Aim', 6, 1),
(7, '潮流匹克球/壁球', 'Opportunity', 7, 1),
(8, '瑜伽与普拉提工坊', 'User', 8, 1),
(9, '五人制室内足球场', 'Football', 9, 1),
(10, '智能多功能会议室', 'Monitor', 10, 1);

-- 初始场地数据 (16 大特色场馆)
INSERT INTO `venue` (`id`, `category_id`, `name`, `capacity`, `price_per_hour`, `cover_image`, `facilities`, `description`, `open_time`, `close_time`, `status`) VALUES
(1, 1, '羽毛球 1 号场 (奥运专业地胶)', 4, 60.00, 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,空调恒温,独立休息凳,免费Wi-Fi', '国际比赛级标准场地，灯光柔和防眩晕，适合高水平切磋与日常健身。', '09:00', '22:00', 1),
(2, 1, '羽毛球 2 号场 (双打标准场)', 4, 50.00, 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,更衣室,免费热水', '标准训练场地，通风良好，性价比极高。', '09:00', '22:00', 1),
(3, 1, '羽毛球 3 号场 (进阶训练场)', 4, 50.00, 'https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60', '专业防滑地胶,独立休息区', '适合团队包场或小团体训练，设施齐全。', '09:00', '22:00', 1),
(4, 2, '中心网球 1 号场 (红土体验)', 4, 120.00, 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60', '红土球场,夜间高亮度泛光灯,球童服务', '法网同级红土脚感，减震效果极佳，带给您极致网球享受。', '09:00', '22:00', 1),
(5, 3, '室内篮球半场 A (木地板)', 10, 80.00, 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60', '美职篮同款枫木地板,计分屏,淋浴间', '全馆配备专业新风系统与弹性龙骨木地板，保护膝盖。', '09:00', '22:00', 1),
(6, 10, '云端多媒体会议室 (20人)', 20, 150.00, 'https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=800&auto=format&fit=crop&q=60', '4K双屏投影,远程视频会议系统,茶歇区,电子白板', '高规格商务路演与研讨会空间，静音隔音，配备千兆专线。', '09:00', '22:00', 1),
(7, 4, '奥体国际标准恒温泳池 (50米8泳道)', 20, 45.00, 'https://images.unsplash.com/photo-1576610616656-d3aa5d1f4534?w=800&auto=format&fit=crop&q=60', '28℃恒温水质循环,臭氧消毒,国家级救生员,独立干湿分离淋浴,智能锁柜', '国际奥体标准50米8泳道，采用德国双重臭氧+硅藻土水质净化循环系统，水温常年恒定28℃，配备专业救生防护与深水浅水安全隔离带。', '08:00', '22:00', 1),
(8, 4, '亲子水适能与康复浅水池', 10, 55.00, 'https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60', '32℃亲子水温,防滑地垫,儿童浮力教具,亲子独立更衣室', '水深0.8-1.1米阶梯式缓冲设计，32℃舒适水温特别适合少儿水适能启蒙、成人水中康复与亲子互动。', '09:00', '21:00', 1),
(9, 5, '乒乓球 1 号专业比赛场 (红双喜彩虹台)', 4, 35.00, 'https://images.unsplash.com/photo-1534158914592-062992fbe900?w=800&auto=format&fit=crop&q=60', '红双喜金彩虹球台,世乒赛级红色地胶,发球机租借,护眼散光排灯', '国家队训练同款红双喜彩虹球台与专业弹力防滑红地胶，台面弹性均一，灯光无频闪，配独立战术观战椅。', '09:00', '22:00', 1),
(10, 6, '乔氏中式黑八尊享包厢 (金腿球台)', 6, 68.00, 'https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?w=800&auto=format&fit=crop&q=60', '乔氏金腿中式黑八台,英国6811台呢,雅乐美比赛球,独立新风降噪包间', '尊享独立静音包间，配置乔氏金腿石板赛台与英国世锦赛级台呢，走球平直顺滑，带舒适沙发休闲茶歇区。', '10:00', '24:00', 1),
(11, 7, '潮流匹克球 1 号场 (低冲击高弹硬地)', 4, 58.00, 'https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800&auto=format&fit=crop&q=60', '专业硬地丙烯酸面层,低冲击缓冲,匹克球拍免费提供,空调恒温', '全网最火潮流轻运动！低冲击力防扭伤地面，老少皆宜上手极快，适合周末好友聚会双打切磋。', '09:00', '22:00', 1),
(12, 7, '德国 ASB 全透玻璃壁球馆', 2, 70.00, 'https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60', '德国ASB高弹回音前墙,全景钢化玻璃后墙,抗震枫木地板', '高燃卡路里杀手！全封闭玻璃竞技馆，极速回球反弹，高隔音与弹性木地板带给您酣畅淋漓的室内爆发力体验。', '09:00', '22:00', 1),
(13, 8, '光影普拉提大器械核心床私教房', 4, 90.00, 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&auto=format&fit=crop&q=60', '进口普拉提核心床Reformer,凯迪拉克秋千架,空中瑜伽吊床,静音新风', '自然采光与温暖原木格调，配备全套专业普拉提大器械与空中吊绳，静谧私密，专注体态矫正与核心重塑。', '08:30', '21:30', 1),
(14, 9, '绿茵天地五人制室内笼式足球场', 12, 120.00, 'https://images.unsplash.com/photo-1529900241467-172608493132?w=800&auto=format&fit=crop&q=60', 'FIFA认证免充砂人造草皮,高弹缓冲垫,高强防护阻燃围网,LED无影高亮球场灯', '免充砂环保草皮脚感逼真防刮伤，全封闭防冲撞缓冲围网，支持业余联赛、企业团建争霸赛与青训集训。', '09:00', '22:00', 1),
(15, 2, '中心网球 2 号场 (澳网同款快速硬地)', 4, 100.00, 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60', '12层高弹丙烯酸地坪,无盲区夜场照明,发球测速雷达', '澳网蓝同款弹性硬地面层，球速快弹跳规则，适合底线大力击球与发球上网战术。', '09:00', '22:00', 1),
(16, 3, '室内篮球赛事级全场 (奥运木地板)', 20, 180.00, 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60', '双龙骨枫木减震地板,可升降液压篮架,4K大屏计时计分,全场更衣淋浴', '国际篮联认证专业级全场，专业无眩光高悬球场灯光，适合中大型企事业单位篮球联赛与商业表演赛。', '09:00', '22:00', 1);

-- 初始模拟预约订单 (带 6 位核销码)
INSERT INTO `booking_order` (`id`, `order_no`, `user_id`, `venue_id`, `book_date`, `time_slot`, `total_amount`, `pay_status`, `order_status`, `verify_code`, `contact_name`, `contact_phone`, `pay_time`, `create_time`) VALUES
(1, 'ORD20260905001', 2, 1, CURRENT_DATE(), '10:00-11:00', 60.00, 1, 1, '839201', '张小凡', '13912345678', NOW(), NOW()),
(2, 'ORD20260905002', 2, 1, CURRENT_DATE(), '14:00-15:00', 60.00, 1, 2, '519302', '张小凡', '13912345678', NOW(), NOW());

-- 初始美团团购风真实口碑评价 (覆盖 1~16 号全量场馆)
INSERT INTO `order_review` (`id`, `order_id`, `venue_id`, `user_id`, `rating`, `env_rating`, `facility_rating`, `service_rating`, `tags`, `content`, `images`, `merchant_reply`, `likes`, `create_time`) VALUES
(1, 10001, 1, 2, 5, 5, 5, 5, '奥运专业地胶,防眩光灯光,恒温空调,脚感减震', '国际比赛级李宁专业地胶脚感太顶了！防滑抓地一流，完全不打滑，侧排防眩光灯光高远球完全不晃眼。馆里24小时新风恒温，打满两小时完全不闷，大汗淋漓太痛快了！', 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60', '【店长回复】：感谢球友的专业深度好评！1号场专为高水平竞技打造，龙骨弹力减震与德国防眩光排灯都是对标国家队赛场标准，期待常来挥拍！', 42, NOW()),
(2, 10002, 1, 5, 5, 5, 5, 5, '更衣室干净,热水淋浴舒适,前台热情,停车免费', '带着全套装备和朋友打双打，地板弹性对膝盖非常友好，急停变向很有安全感。淋浴间水压大而且恒温，吹风机大风力很赞。出门扫码领了两小时免费停车券，完美！', 'https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60', '【店长回复】：您的全方位满意是我们最高追求！我们提供恒温热水及两小时停车减免，期待下次继续为您服务！', 26, NOW()),
(3, 10003, 1, 1, 5, 5, 5, 4, '智能闸机极速入场,免排队,场地划线清晰', '微信订场直接生成6位核销码，闸机一刷秒通行，整个流程没有多余废话。线位边缘整洁，球网绷得紧挺，硬件和软件都在线的高品质球馆。', '', '【店长回复】：感谢体验！智能物联无人值守与极速出票正是我们的特色，祝您挥拍畅快！', 19, NOW()),

(4, 10004, 2, 2, 5, 5, 5, 5, '超高挑高,视野开阔,性价比之王,双打首选', '挑高足足有9.5米，后场起跳大力扣杀完全不担心蹭到顶棚灯带。场地间距宽敞，跑动完全不会影响邻场，每小时50块的性价比真的没谁了，强烈推荐打双打！', 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60', '【店长回复】：感谢球友推荐！2号场专门拓宽了外沿缓冲区，给双打大范围跑位留足了空间，性价比常青树！', 31, NOW()),
(5, 10005, 2, 5, 5, 5, 4, 5, '场地整洁,保洁阿姨勤快,平价功能饮品', '场地上一点灰尘都没有，保洁阿姨每隔一个小时就用专业静电拖把推尘，地胶抓地感很踏实。前台自动售货机佳得乐和宝矿力价格跟超市一样良心，必须点赞！', '', '【店长回复】：卫生第一，细节致胜！平价冷饮和整洁环境是我们的基本原则，欢迎常来！', 15, NOW()),

(6, 10006, 3, 5, 5, 5, 5, 5, '录像战术支架,多机位复盘,训练氛围拉满,发球机好用', '球场边居然自带多角度手机训练录像支架，还有战术小白板！自己架上手机录了正手起跳劈吊和滑板吊球，回去一帧一帧复盘，球技提升太快了。前台还可以租借全自动发球机！', 'https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60', '【店长回复】：太懂球了！3号场就是为想要突破瓶颈期的进阶球友量身打造的\'实验室\'，欢迎多来录制高光集锦！', 35, NOW()),
(7, 10007, 3, 2, 5, 4, 5, 5, '青训安全推荐,多层复合减震,保护膝盖', '带队里几个小队员集训，地胶下层做了多层复合龙骨减震，连续练两个小时步伐脚底板和跟腱都不酸，安全系数很高，家长们看了也很满意。', '', '【店长回复】：科学运动，保护运动寿命！我们的弹性减震经过专业机构认证，期待更多小冠军从这里走出！', 20, NOW()),

(8, 10008, 4, 1, 5, 5, 5, 5, '法网红土同款,滑步极致丝滑,高亮度夜场泛光,尊贵仪式感', '全市找不出第二家这么正宗的红土球场！颗粒均匀细致，法网红土同款滑步手感让人上瘾，急停滑行卸力极其自然。晚上八盏大功率无频闪泛光灯一开，比白天视线还清晰，尊崇感直接拉满！', 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60', '【店长回复】：感谢网球资深爱好者的最高嘉奖！我们特意采购纯正进口天然红砖陶粒粉，每日赛后雾化喷淋养护并专业平整，为您保留最醇厚的罗兰加洛斯红土质感！', 58, NOW()),
(9, 10009, 4, 5, 5, 5, 5, 5, '五星级服务,提供专用扫线器,温热毛巾,球童贴心', '跟私教练发球和底线正反手相持，打完后工作人员贴心送上冰镇苏打水和热毛巾，还主动帮忙整理球框。换衣洗漱出来神清气爽，这120块一小时花得太值了！', '', '【店长回复】：细致关怀是我们的标准流程。红土球场不仅是运动场，更是尊享社交空间，期待再次接待您！', 29, NOW()),

(10, 10010, 5, 2, 5, 5, 5, 5, 'NBA级实木地板,急停变向不打滑,篮网声音清脆,空调劲爆', '美职篮同款北美枫木双层龙骨地板，落地完全没有生硬反震，鞋底摩擦声音超级清脆吱滋叫！篮圈弹性刚刚好，链网加织网进球声音太治愈了，全场空调给力，夏天打球居然还会觉得凉快！', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1519766304817-4f37bda74a29?w=800&auto=format&fit=crop&q=60', '【店长回复】：听到空心入网那声脆响就是球场最美的乐章！我们地板每月定期打专业防滑耐磨养护蜡，欢迎带兄弟们常来约战！', 47, NOW()),
(11, 10011, 5, 5, 5, 5, 5, 5, '独立新风充足,手机蓝牙投屏计分,无盲区抓拍', '我们部门五打五半场争霸赛，LED电子大屏可以直接连手机蓝牙计分、放队歌！全馆新风系统换气速度极快，打到最后大家一点都没觉得缺氧，气氛燃爆了！', '', '【店长回复】：团建选这里太有眼光了！智能计分大屏与大风量新风正是为热血赛事定制，期待贵司下次再办联赛！', 33, NOW()),

(12, 10012, 6, 1, 5, 5, 5, 5, '4K高清双屏,静音隔音极佳,千兆专线稳定,现磨咖啡香醇', '公司季度高管战略闭门研讨会选在这里，效果超出预期！双屏4K激光投影无缝分屏展示财务报表与代码架构，无线投屏丝毫没有卡顿。双层真空隔音玻璃，门一关外面鸦雀无声，私密性极佳！', 'https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=800&auto=format&fit=crop&q=60', '【店长回复】：感谢战略客户的高规格信赖！云端会议空间专为高端商务、投融资路演与闭门研讨量身设计，配备千兆独立光纤与智能静音系统，助您商战捷报频传！', 39, NOW()),
(13, 10013, 6, 5, 5, 5, 5, 5, '人体工学皮椅,智能电子白板,茶歇精致,前台体面', '用来组织了一场技术沙龙交流，交互式电子白板直接可以扫码把板书笔记带走，椅子是赫曼米勒同款工学椅，连续坐了四个小时腰一点不酸，参会嘉宾纷纷夸赞场地规格高级。', '', '【店长回复】：能够为高水平技术沙龙赋能是我们的荣幸！电子白板一键扫码存云端就是为了无纸化高效协作，期待常来！', 24, NOW()),

(14, 10014, 7, 5, 5, 5, 5, 5, '恒温水质清澈,臭氧消毒无氯味,救生员专业负责,淋浴水流强劲', '水质非常清亮见底，完全没有传统泳池刺鼻的氯气味！50米大池游起来太舒展了，水温常年28度很舒适。淋浴间水压超足，更衣柜电子感应很方便。', 'https://images.unsplash.com/photo-1576610616656-d3aa5d1f4534?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60', '【店长回复】：感谢泳友的高度评价！我们采用德国逆流式循环与臭氧辅以硅藻土物理过滤，全天候保持国际泳联竞赛级水质标准，欢迎常来畅游！', 45, NOW()),
(15, 10015, 7, 2, 5, 5, 5, 5, '泳道宽敞不挤,深水区视线清晰,智能手环储物', '深水区划线非常醒目，救生员一直来回巡视安全感十足。晚上来游人也不挤，一人一个泳道太爽了，游完在休息区喝一杯电解质水特别舒服。', '', '【店长回复】：安全与畅快是我们的核心承诺！泳道分流管控保证每位泳者拥有独立舒适的行进空间，期待下次光临！', 22, NOW()),

(16, 10016, 8, 5, 5, 5, 5, 5, '32℃温水舒适,儿童浮水教具齐全,亲子独立更衣室贴心', '带5岁宝宝来上启蒙水适能课，水温32度很温暖，完全不用担心孩子着凉！防滑地垫铺得很满，还提供各种可爱的浮力背心和水上玩具，宝宝玩得超级开心！', 'https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60', '【店长回复】：看到小朋友玩得开心就是我们最欣慰的事！亲子水域专设阶梯式缓坡水深与恒定32℃暖水，呵护孩子每一步水上成长！', 38, NOW()),
(17, 10017, 8, 2, 5, 5, 5, 5, '水中康复舒缓,无障碍坡道友好,教练耐心细致', '术后做膝关节水中抗阻康复训练，水深刚好到胸口，浮力卸掉身体重量后走动完全不痛。教练很专业，全程指导姿势，环境安静不嘈杂。', '', '【店长回复】：祝您身体早日康复如初！浅水池专为低负荷水动能康复设计，我们会持续为您提供细致温和的环境！', 19, NOW()),

(18, 10018, 9, 2, 5, 5, 5, 5, '红双喜金彩虹台,红地胶抓地稳,发球机练球神仙搭配', '球台弹性均匀无死角，台面磨砂摩擦力极好，拉弧圈球旋转强烈！地胶踩上去很弹很稳。前台租的发球机可以自编旋转和落点，自己一个人练了俩小时正反手衔接，大汗淋漓！', 'https://images.unsplash.com/photo-1534158914592-062992fbe900?w=800&auto=format&fit=crop&q=60', '【店长回复】：老球手一上手就知道分量！国家队世乒赛同款彩虹台搭配智能发球机，就是为独行侠球友打造的练级圣地，欢迎常来切磋！', 33, NOW()),
(19, 10019, 9, 5, 5, 5, 4, 5, '防眩漫反射顶灯,独立战术观战椅,性价比极高', '灯光设计很懂乒乓球，漫反射完全不晃眼睛，打高球也不会丢失视野。边上有专门放毛巾和水壶的置物架，35块钱一小时在市区简直是神仙价格。', '', '【店长回复】：乒乓国球就该全民普惠！专业级照明与高性价比是我们对球友的长情陪伴，感谢认可！', 25, NOW()),

(20, 10020, 10, 1, 5, 5, 5, 5, '乔氏金腿石板极平整,英国6811台呢走球丝滑,雅乐美比赛球', '台球发烧友必来！乔氏金腿真材实料，石板调校得分毫不差，慢速库边反弹角度精准无比。雅乐美TV比赛球撞击声音清脆通透，独立包厢带新风抽风机，完全没异味！', 'https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?w=800&auto=format&fit=crop&q=60', '【店长回复】：遇上行家里手了！英国进口6811台呢与高精度温控石板每周定期精密调平，给每一杆走位最细腻的物理反馈！', 52, NOW()),
(21, 10021, 10, 5, 5, 5, 5, 5, '商务接待有面子,皮质沙发茶歇舒适,现磨咖啡正宗', '请客户来打台球谈业务，包厢私密性极好，门一关静音效果出色。前台送来的现磨美式咖啡很纯正，沙发坐着很舒服，客户赞不绝口，合同也顺利推进了！', '', '【店长回复】：恭喜老板业务长虹！台球与商务社交在此完美融合，竭诚为您打造高品位会客体验！', 28, NOW()),

(22, 10022, 11, 5, 5, 5, 5, 5, '匹克球上手极快,免费借碳纤维球拍,动感音乐气氛超嗨', '最近全网爆火的匹克球终于体验到了！零基础小白5分钟就学会了发球和截击，比网球容易上手，比羽毛球更适合社交。前台小哥免费借了碳纤维球拍，还送了吸汗带，太暖心了！', 'https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800&auto=format&fit=crop&q=60', '【店长回复】：匹克球的快乐就是这么直接！低门槛、高热量消耗和欢乐社交，下周五还有匹克球单身盲盒切磋夜，期待您参与！', 41, NOW()),
(23, 10023, 11, 2, 5, 5, 5, 4, '丙烯酸面层减震好,室内空调给力,聚会团建不二之选', '跟几个大学同学聚会选在这，球场画线清晰，场地回弹适中对脚踝很友好。全场冷气开得很足，连续打两小时一点不闷，已经约了下周继续包场！', '', '【店长回复】：青春就是挥汗如雨的欢笑！丙烯酸多层缓冲面层专门减轻了半月板负荷，祝你们友谊长青！', 17, NOW()),

(24, 10024, 12, 1, 5, 5, 5, 5, '全透钢化玻璃视觉震撼,ASB前墙击球回音超爽,减压天花板', '下班后极度解压！全透明玻璃设计科技感爆棚，壁球打在ASB墙上那声‘咚’的闷响太让人上瘾了！40分钟心率飙到160，暴汗排毒，浑身通透！', 'https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60', '【店长回复】：高压都市生活最好的情绪解药！德国原装ASB专业回音弹性墙体，让每一次挥拍击球都成为力量的彻底释放！', 49, NOW()),
(25, 10025, 12, 5, 5, 5, 5, 5, '枫木抗震地板很弹,更衣淋浴干净,备有护目镜很专业', '新手第一次打壁球，前台非常负责地强调必须佩戴护目镜并免费提供。抗震地板落地很轻盈，不会震得脑仁疼，洗澡水温水压很赞！', '', '【店长回复】：安全是竞技的第一准则！专业护目镜与高弹地板为您的极速变向提供周密防护，期待常来！', 26, NOW()),

(26, 10026, 13, 5, 5, 5, 5, 5, '进口Reformer核心床丝滑,原木风光影超美,私密安静出片', '环境美得像艺术馆！下午阳光斜照进来光影绝美，随手一拍发小红书赞爆。进口核心床滑轨顺滑无声，弹簧阻力细腻均匀，做百次拍打和卷腹时核心激活特别深！', 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&auto=format&fit=crop&q=60', '【店长回复】：普拉提是身体与心灵的建筑学。静谧的原木自然光影搭配顶级核心床器械，让每一次呼吸与延展都成为享受！', 63, NOW()),
(27, 10027, 13, 2, 5, 5, 5, 5, '凯迪拉克秋千架齐全,脊柱拉伸改善体态,空气新风清新', '长期久坐腰肌劳损，来做了针对骨盆后倾和胸椎灵活性训练。凯迪拉克床配件很全，练完拉伸下来感觉整个人长高了2厘米！独立房间非常私密，没有异味。', '', '【店长回复】：改善体态、赋能活力正是普拉提的核心魅力！坚持练习，身体会给您最惊喜的正向反馈！', 31, NOW()),

(28, 10028, 14, 2, 5, 5, 5, 5, '免充砂环保草皮脚感极软,无盲区LED大灯,围网结实防飞球', '周五晚上公司足球俱乐部约战！免充砂环保草皮脚感太舒服了，鞋钉抓地牢固，铲球完全不划破皮。全包围网非常结实，踢飞的球瞬间回弹，比赛节奏极快！', 'https://images.unsplash.com/photo-1529900241467-172608493132?w=800&auto=format&fit=crop&q=60', '【店长回复】：无热血，不足球！免充砂草皮专为室内快节奏短传配合设计，环保无异味防擦伤，欢迎常来踢夜场！', 56, NOW()),
(29, 10029, 14, 5, 5, 5, 5, 5, '更衣室配大储物柜,冷饮充足,组织比赛很省心', '组织了一场校友五人制对抗赛，球场自带电子计分板和替换背心。踢完大家冲个热水澡神清气爽，停车也很方便，大家都说下次还定这里！', '', '【店长回复】：校友欢聚，球场添彩！我们提供全套赛事物料支持与便捷泊车，期待为您的下一次联赛服务！', 27, NOW()),

(30, 10030, 15, 1, 5, 5, 5, 5, '澳网经典蓝硬地,球速快弹跳规整,发球测速雷达高级', '纯正澳网同款硬地！球弹起来非常规整，没有死角怪弹，底线发力击球的声音像炮弹一样带感。场边还架设了发球测速雷达，今天一发测到178km/h，打得超兴奋！', 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60', '【店长回复】：178km/h一发堪比职业水准！澳网蓝快速硬地面层与专业测速雷达就是为硬核竞技而生，期待您刷新个人时速纪录！', 44, NOW()),
(31, 10031, 15, 5, 5, 5, 4, 5, '夜场泛光无阴影,球网标准紧挺,休息长椅干净', '晚上和教练练底线截击，大功率高悬照明完全照亮全场，没有视野死角。球场排水坡度做得很好，地面干燥防滑，体验挑不出毛病。', '', '【店长回复】：专注细节，精益求精！专业级照明与严谨平整度保障您的每一次发球上网，祝球技精进！', 21, NOW()),

(32, 10032, 16, 2, 5, 5, 5, 5, '国际篮联双龙骨枫木,液压篮架极稳固,4K大屏比赛氛围超燃', '行业邀请赛总决赛定在这里办，真的有打NBA的感觉！双龙骨枫木地板弹性极佳，落地膝盖毫无冲击感。扣篮挂筐液压篮架纹丝不动，LED大屏滚动放比分和集锦，观众席坐满呼声雷动！', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60', '【店长回复】：热血澎湃的经典对决！赛事级实木全场与液压竞赛篮架专为高强度对抗而生，恭喜冠军战队，期待更多辉煌赛事在此诞生！', 68, NOW()),
(33, 10033, 16, 5, 5, 5, 5, 5, '中央空调全馆凉爽,更衣室淋浴喷头水量超大,安保专业体面', '包全场打了两小时全场对抗，全馆中央空调完全顶得住二十个人的热量，一点都不闷热。打完洗澡水温恒定，喷头水量大，出来前台还主动帮忙开电子发票，正规体面！', '', '【店长回复】：专业保障，用心护航！大制冷量中央空调与高标准卫浴系统全力护航赛事与团建，期待常来！', 35, NOW());

-- ----------------------------
-- 7. 营销优惠券表 (coupon)
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `code` VARCHAR(32) NOT NULL UNIQUE COMMENT '券批次码',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '1-满减券, 2-折扣券, 3-无门槛立减券',
  `min_spend` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '最低使用门槛金额',
  `discount_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '减免金额',
  `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '折扣比例',
  `valid_days` INT NOT NULL DEFAULT 30 COMMENT '有效天数',
  `total_count` INT NOT NULL DEFAULT 1000 COMMENT '发放总量',
  `claimed_count` INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `description` VARCHAR(255) DEFAULT '' COMMENT '使用说明',
  `category_id` BIGINT DEFAULT NULL COMMENT '限定品类ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常发放, 0-停发',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销优惠券模板表';

-- ----------------------------
-- 8. 用户领券记录表 (user_coupon)
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
  `user_id` BIGINT NOT NULL COMMENT '领取用户ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未使用, 1-已使用, 2-已过期',
  `claim_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` DATETIME NOT NULL,
  `used_time` DATETIME DEFAULT NULL,
  `order_no` VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领券记录表';

-- ----------------------------
-- 9. 拼场招募活动表 (match_activity)
-- ----------------------------
DROP TABLE IF EXISTS `match_activity`;
CREATE TABLE `match_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '拼场业务单号',
  `creator_id` BIGINT NOT NULL COMMENT '发起人ID',
  `venue_id` BIGINT NOT NULL COMMENT '场馆ID',
  `venue_name` VARCHAR(64) NOT NULL COMMENT '场馆名称',
  `category_name` VARCHAR(64) NOT NULL COMMENT '运动分类',
  `book_date` DATE NOT NULL COMMENT '活动日期',
  `time_slot` VARCHAR(32) NOT NULL COMMENT '活动时段',
  `title` VARCHAR(100) NOT NULL COMMENT '拼场主题',
  `sport_tag` VARCHAR(64) DEFAULT '双打AA' COMMENT '运动标签',
  `target_members` INT NOT NULL DEFAULT 4 COMMENT '目标招募人数',
  `current_members` INT NOT NULL DEFAULT 1 COMMENT '当前已参与人数',
  `total_amount` DECIMAL(8,2) NOT NULL COMMENT '场地总费用',
  `cost_per_person` DECIMAL(8,2) NOT NULL COMMENT '人均AA费用',
  `description` TEXT COMMENT '活动要求说明',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-招募中, 1-拼场成功已出票, 2-已核销, 3-已解散退款',
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_venue_date` (`venue_id`, `book_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼场约球活动表';

-- ----------------------------
-- 10. 拼场成员表 (match_participant)
-- ----------------------------
DROP TABLE IF EXISTS `match_participant`;
CREATE TABLE `match_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '拼场活动ID',
  `user_id` BIGINT NOT NULL COMMENT '参与用户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '头像',
  `pay_amount` DECIMAL(8,2) NOT NULL COMMENT '支付金额',
  `pay_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-已支付, 2-已退款',
  `is_creator` TINYINT NOT NULL DEFAULT 0 COMMENT '1-发起人, 0-普通成员',
  `verify_code` VARCHAR(16) DEFAULT NULL COMMENT '专属到场核销码',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼场成员表';

-- 初始优惠券模板
INSERT INTO `coupon` (`id`, `name`, `code`, `type`, `min_spend`, `discount_amount`, `discount_rate`, `valid_days`, `total_count`, `claimed_count`, `description`, `status`) VALUES
(1, '新人尊享立减券 (无门槛)', 'NEWUSER20', 3, 0.00, 20.00, 1.00, 30, 5000, 128, '注册会员专属福利，全场任意场馆下单立减 20 元', 1),
(2, '夜间黄金档满减神券', 'NIGHT100_25', 1, 100.00, 25.00, 1.00, 15, 2000, 350, '晚间 18:00~22:00 高峰期消费满 100 元立减 25 元', 1),
(3, '周末燃动畅玩 8.5 折特惠券', 'WEEKEND85', 2, 50.00, 0.00, 0.85, 30, 3000, 512, '周六日全品类通用，单笔订单满 50 元享受 8.5 折优惠', 1),
(4, '羽网球友专项满减券', 'RACKET60_15', 1, 60.00, 15.00, 1.00, 20, 1500, 180, '羽毛球馆与网球中心专属，满 60 元立减 15 元', 1);

-- 初始拼场活动
INSERT INTO `match_activity` (`id`, `activity_no`, `creator_id`, `venue_id`, `venue_name`, `category_name`, `book_date`, `time_slot`, `title`, `sport_tag`, `target_members`, `current_members`, `total_amount`, `cost_per_person`, `description`, `status`, `expire_time`, `create_time`) VALUES
(1, 'ACT20260908001', 2, 1, '羽毛球 1 号场 (奥运专业地胶)', '羽毛球馆', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), '19:00-20:00', '周三晚李宁双打进阶局【缺2人】AA制15元/位', '双打进阶·AA畅打', 4, 2, 60.00, 15.00, '自带红胜利羽毛球，水平4.0左右，拒绝划水，激战一小时大汗淋漓！', 0, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW()),
(2, 'ACT20260908002', 5, 4, '中心网球 1 号场 (红土体验)', '网球中心', DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY), '15:00-16:00', '罗兰加洛斯红土拉球局！求一稳定底线球友', '红土底线·新手包容', 2, 1, 120.00, 60.00, '提供法网同款比赛球，练习正反手稳定对拉，欢迎爱好网球的朋友切磋！', 0, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW()),
(3, 'ACT20260908003', 2, 5, '室内篮球半场 A (木地板)', '篮球全场/半场', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), '18:00-19:00', '下班解压！室内半场 3v3 热血投篮对抗赛', '热血对抗·3v3半场', 6, 5, 80.00, 13.33, '木地板防滑减震，自带球衣背心，还差最后 1 位神射手马上发车！', 0, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW()),
(4, 'ACT20260908004', 5, 11, '潮流匹克球 1 号场 (低冲击高弹硬地)', '潮流匹克球/壁球', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), '20:00-21:00', '全网爆火匹克球破冰局！零基础小白新手友好', '潮流轻运动·新手友好', 4, 4, 58.00, 14.50, '馆里免费借碳纤维球拍，5分钟包教包会，轻松出汗交朋友！', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW());

-- 初始拼场成员
INSERT INTO `match_participant` (`id`, `activity_id`, `user_id`, `username`, `nickname`, `avatar`, `pay_amount`, `pay_status`, `is_creator`, `verify_code`, `join_time`) VALUES
(1, 1, 2, 'user', '羽球小旋风', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 15.00, 1, 1, NULL, NOW()),
(2, 1, 5, 'user1', '先锋运动会员', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 15.00, 1, 0, NULL, NOW()),
(3, 2, 5, 'user1', '先锋运动会员', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 60.00, 1, 1, NULL, NOW()),
(4, 3, 2, 'user', '羽球小旋风', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 13.33, 1, 1, NULL, NOW()),
(5, 3, 1, 'admin', '系统超级管理员', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 13.33, 1, 0, NULL, NOW()),
(6, 4, 5, 'user1', '先锋运动会员', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 14.50, 1, 1, '712903', NOW()),
(7, 4, 2, 'user', '羽球小旋风', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', 14.50, 1, 0, '684912', NOW());

SET FOREIGN_KEY_CHECKS = 1;
