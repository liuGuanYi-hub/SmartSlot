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
        initUserCreditScoreColumn();
        initOrderReviewColumns();
        initRolesAndUsers();
        initSportsCategoriesAndVenues();
        fixVenueCoverImages();
        initVenueReviews();
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

    private void initUserCreditScoreColumn() {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'credit_score'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `credit_score` INT NOT NULL DEFAULT 100 COMMENT '履约信用分'");
                log.info("数据库初始化: sys_user 成功扩展 credit_score 履约信用分字段");
            }
        } catch (Exception e) {
            log.warn("检查或添加 credit_score 字段异常: {}", e.getMessage());
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

            // 3. 先锋运动会员 user1
            SysUser user1 = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "user1"));
            if (user1 == null) {
                sysUserService.save(SysUser.builder()
                        .username("user1")
                        .password(defaultPwd)
                        .nickname("先锋运动会员")
                        .phone("13800003333")
                        .role(UserRole.ROLE_USER)
                        .balance(new BigDecimal("1000.00"))
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build());
                log.info("初始化体验用户: user1 / 123456 (ROLE_USER)");
            }
        } catch (Exception e) {
            log.warn("初始化 RBAC 默认用户异常 (若库不可写可忽略): {}", e.getMessage());
        }
    }

    private void initSportsCategoriesAndVenues() {
        try {
            // 1. 扩充更新 10 大运动场馆分类
            String[][] categories = {
                {"1", "羽毛球馆", "Basketball", "1"},
                {"2", "网球中心", "Trophy", "2"},
                {"3", "篮球全场/半场", "Football", "3"},
                {"4", "恒温游泳水上馆", "Watermelon", "4"},
                {"5", "乒乓球中心", "Medal", "5"},
                {"6", "台球/斯诺克俱乐部", "Aim", "6"},
                {"7", "潮流匹克球/壁球", "Opportunity", "7"},
                {"8", "瑜伽与普拉提工坊", "User", "8"},
                {"9", "五人制室内足球场", "Football", "9"},
                {"10", "智能多功能会议室", "Monitor", "10"}
            };

            for (String[] c : categories) {
                String sql = "INSERT INTO `venue_category` (`id`, `name`, `icon`, `sort`, `status`) VALUES (?, ?, ?, ?, 1) " +
                             "ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `icon` = VALUES(`icon`), `sort` = VALUES(`sort`)";
                jdbcTemplate.update(sql, Long.parseLong(c[0]), c[1], c[2], Integer.parseInt(c[3]));
            }
            log.info("数据库初始化: 10 大体育运动场馆分类扩充同步就绪");

            // 将原有 6 号会议室迁移至 10 号智能多功能会议室分类
            jdbcTemplate.update("UPDATE `venue` SET `category_id` = 10 WHERE `id` = 6");

            // 2. 扩充更新 16 大特色运动场馆
            Object[][] venues = {
                {
                    7L, 4L, "奥体国际标准恒温泳池 (50米8泳道)", 20, new BigDecimal("45.00"),
                    "https://images.unsplash.com/photo-1576610616656-d3aa5d1f4534?w=800&auto=format&fit=crop&q=60",
                    "28℃恒温水质循环,臭氧消毒,国家级救生员,独立干湿分离淋浴,智能锁柜",
                    "国际奥体标准50米8泳道，采用德国双重臭氧+硅藻土水质净化循环系统，水温常年恒定28℃，配备专业救生防护与深水浅水安全隔离带。",
                    "08:00", "22:00"
                },
                {
                    8L, 4L, "亲子水适能与康复浅水池", 10, new BigDecimal("55.00"),
                    "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60",
                    "32℃亲子水温,防滑地垫,儿童浮力教具,亲子独立更衣室",
                    "水深0.8-1.1米阶梯式缓冲设计，32℃舒适水温特别适合少儿水适能启蒙、成人水中康复与亲子互动。",
                    "09:00", "21:00"
                },
                {
                    9L, 5L, "乒乓球 1 号专业比赛场 (红双喜彩虹台)", 4, new BigDecimal("35.00"),
                    "https://images.unsplash.com/photo-1534158914592-062992fbe900?w=800&auto=format&fit=crop&q=60",
                    "红双喜金彩虹球台,世乒赛级红色地胶,发球机租借,护眼散光排灯",
                    "国家队训练同款红双喜彩虹球台与专业弹力防滑红地胶，台面弹性均一，灯光无频闪，配独立战术观战椅。",
                    "09:00", "22:00"
                },
                {
                    10L, 6L, "乔氏中式黑八尊享包厢 (金腿球台)", 6, new BigDecimal("68.00"),
                    "https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?w=800&auto=format&fit=crop&q=60",
                    "乔氏金腿中式黑八台,英国6811台呢,雅乐美比赛球,独立新风降噪包间",
                    "尊享独立静音包间，配置乔氏金腿石板赛台与英国世锦赛级台呢，走球平直顺滑，带舒适沙发休闲茶歇区。",
                    "10:00", "24:00"
                },
                {
                    11L, 7L, "潮流匹克球 1 号场 (低冲击高弹硬地)", 4, new BigDecimal("58.00"),
                    "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800&auto=format&fit=crop&q=60",
                    "专业硬地丙烯酸面层,低冲击缓冲,匹克球拍免费提供,空调恒温",
                    "全网最火潮流轻运动！低冲击力防扭伤地面，老少皆宜上手极快，适合周末好友聚会双打切磋。",
                    "09:00", "22:00"
                },
                {
                    12L, 7L, "德国 ASB 全透玻璃壁球馆", 2, new BigDecimal("70.00"),
                    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60",
                    "德国ASB高弹回音前墙,全景钢化玻璃后墙,抗震枫木地板",
                    "高燃卡路里杀手！全封闭玻璃竞技馆，极速回球反弹，高隔音与弹性木地板带给您酣畅淋漓的室内爆发力体验。",
                    "09:00", "22:00"
                },
                {
                    13L, 8L, "光影普拉提大器械核心床私教房", 4, new BigDecimal("90.00"),
                    "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&auto=format&fit=crop&q=60",
                    "进口普拉提核心床Reformer,凯迪拉克秋千架,空中瑜伽吊床,静音新风",
                    "自然采光与温暖原木格调，配备全套专业普拉提大器械与空中吊绳，静谧私密，专注体态矫正与核心重塑。",
                    "08:30", "21:30"
                },
                {
                    14L, 9L, "绿茵天地五人制室内笼式足球场", 12, new BigDecimal("120.00"),
                    "https://images.unsplash.com/photo-1529900241467-172608493132?w=800&auto=format&fit=crop&q=60",
                    "FIFA认证免充砂人造草皮,高弹缓冲垫,高强防护阻燃围网,LED无影高亮球场灯",
                    "免充砂环保草皮脚感逼真防刮伤，全封闭防冲撞缓冲围网，支持业余联赛、企业团建争霸赛与青训集训。",
                    "09:00", "22:00"
                },
                {
                    15L, 2L, "中心网球 2 号场 (澳网同款快速硬地)", 4, new BigDecimal("100.00"),
                    "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60",
                    "12层高弹丙烯酸地坪,无盲区夜场照明,发球测速雷达",
                    "澳网蓝同款弹性硬地面层，球速快弹跳规则，适合底线大力击球与发球上网战术。",
                    "09:00", "22:00"
                },
                {
                    16L, 3L, "室内篮球赛事级全场 (奥运木地板)", 20, new BigDecimal("180.00"),
                    "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60",
                    "双龙骨枫木减震地板,可升降液压篮架,4K大屏计时计分,全场更衣淋浴",
                    "国际篮联认证专业级全场，专业无眩光高悬球场灯光，适合中大型企事业单位篮球联赛与商业表演赛。",
                    "09:00", "22:00"
                }
            };

            String venueSql = "INSERT INTO `venue` (`id`, `category_id`, `name`, `capacity`, `price_per_hour`, `cover_image`, `facilities`, `description`, `open_time`, `close_time`, `status`) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1) " +
                              "ON DUPLICATE KEY UPDATE `category_id`=VALUES(`category_id`), `name`=VALUES(`name`), `capacity`=VALUES(`capacity`), `price_per_hour`=VALUES(`price_per_hour`), `cover_image`=VALUES(`cover_image`), `facilities`=VALUES(`facilities`), `description`=VALUES(`description`), `open_time`=VALUES(`open_time`), `close_time`=VALUES(`close_time`)";

            for (Object[] v : venues) {
                jdbcTemplate.update(venueSql, v);
            }
            log.info("数据库初始化: 全量 16 大特色体育运动场馆扩充入库完成");
        } catch (Exception e) {
            log.warn("初始化体育分类与场馆数据异常: {}", e.getMessage());
        }
    }

    private void fixVenueCoverImages() {
        try {
            String sql = "UPDATE `venue` SET `cover_image` = 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60' WHERE `id` = 5 OR `cover_image` LIKE '%1505666287802%'";
            jdbcTemplate.execute(sql);
            log.info("数据库初始化: 场地封面图片一致性检查修复完毕 (已将 404 图片替换为有效高清图)");
        } catch (Exception e) {
            log.warn("检查或更新场地封面图片异常: {}", e.getMessage());
        }
    }

    private void initOrderReviewColumns() {
        try {
            try {
                jdbcTemplate.execute("ALTER TABLE `order_review` DROP INDEX `uk_order`");
            } catch (Exception ignored) {}

            String[] colNames = {"env_rating", "facility_rating", "service_rating", "tags", "images", "merchant_reply", "likes"};
            String[] colSqls = {
                "ALTER TABLE `order_review` ADD COLUMN `env_rating` INT NOT NULL DEFAULT 5 COMMENT '环境评分'",
                "ALTER TABLE `order_review` ADD COLUMN `facility_rating` INT NOT NULL DEFAULT 5 COMMENT '设施评分'",
                "ALTER TABLE `order_review` ADD COLUMN `service_rating` INT NOT NULL DEFAULT 5 COMMENT '服务评分'",
                "ALTER TABLE `order_review` ADD COLUMN `tags` VARCHAR(255) DEFAULT '' COMMENT '特色评价标签'",
                "ALTER TABLE `order_review` ADD COLUMN `images` TEXT DEFAULT NULL COMMENT '实拍晒图URL集合'",
                "ALTER TABLE `order_review` ADD COLUMN `merchant_reply` VARCHAR(500) DEFAULT NULL COMMENT '商家回复'",
                "ALTER TABLE `order_review` ADD COLUMN `likes` INT NOT NULL DEFAULT 0 COMMENT '有用/点赞数'"
            };

            for (int i = 0; i < colNames.length; i++) {
                String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_review' AND COLUMN_NAME = '" + colNames[i] + "'";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
                if (count == null || count == 0) {
                    jdbcTemplate.execute(colSqls[i]);
                    log.info("数据库初始化: order_review 成功扩展 {} 字段", colNames[i]);
                }
            }
        } catch (Exception e) {
            log.warn("检查或扩展 order_review 字段异常: {}", e.getMessage());
        }
    }

    private void initVenueReviews() {
        try {
            SysUser user1 = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "user1"));
            Long user1Id = user1 != null ? user1.getId() : 2L;

            Integer totalReviews = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `order_review`", Integer.class);
            if (totalReviews != null && totalReviews >= 30) {
                jdbcTemplate.update("UPDATE `order_review` SET `user_id` = ? WHERE `user_id` = 5", user1Id);
                log.info("数据库初始化: 场馆全量美团式评价已存在，已校准用户关联 (共 {} 条评价)", totalReviews);
                return;
            }

            // 清理历史极简测试评价并导入全套沉浸式美团点评
            jdbcTemplate.execute("DELETE FROM `order_review` WHERE `id` <= 100 OR `order_id` >= 10000");

            String insertSql = """
                INSERT INTO `order_review` (`order_id`, `venue_id`, `user_id`, `rating`, `env_rating`, `facility_rating`, `service_rating`, `tags`, `content`, `images`, `merchant_reply`, `likes`, `create_time`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            Object[][] reviews = {
                // 场馆 1: 羽毛球 1 号场 (奥运专业地胶)
                {
                    10001L, 1L, 2L, 5, 5, 5, 5,
                    "奥运专业地胶,防眩光灯光,恒温空调,脚感减震",
                    "国际比赛级李宁专业地胶脚感太顶了！防滑抓地一流，完全不打滑，侧排防眩光灯光高远球完全不晃眼。馆里24小时新风恒温，打满两小时完全不闷，大汗淋漓太痛快了！",
                    "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：感谢球友的专业深度好评！1号场专为高水平竞技打造，龙骨弹力减震与德国防眩光排灯都是对标国家队赛场标准，期待常来挥拍！",
                    42, LocalDateTime.now().minusDays(1).minusHours(3)
                },
                {
                    10002L, 1L, 5L, 5, 5, 5, 5,
                    "更衣室干净,热水淋浴舒适,前台热情,停车免费",
                    "带着全套装备和朋友打双打，地板弹性对膝盖非常友好，急停变向很有安全感。淋浴间水压大而且恒温，吹风机大风力很赞。出门扫码领了两小时免费停车券，完美！",
                    "https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：您的全方位满意是我们最高追求！我们提供恒温热水及两小时停车减免，期待下次继续为您服务！",
                    26, LocalDateTime.now().minusDays(2).minusHours(5)
                },
                {
                    10003L, 1L, 1L, 5, 5, 5, 4,
                    "智能闸机极速入场,免排队,场地划线清晰",
                    "微信订场直接生成6位核销码，闸机一刷秒通行，整个流程没有多余废话。线位边缘整洁，球网绷得紧挺，硬件和软件都在线的高品质球馆。",
                    "",
                    "【店长回复】：感谢体验！智能物联无人值守与极速出票正是我们的特色，祝您挥拍畅快！",
                    19, LocalDateTime.now().minusDays(3).minusHours(1)
                },

                // 场馆 2: 羽毛球 2 号场 (双打标准场)
                {
                    10004L, 2L, 2L, 5, 5, 5, 5,
                    "超高挑高,视野开阔,性价比之王,双打首选",
                    "挑高足足有9.5米，后场起跳大力扣杀完全不担心蹭到顶棚灯带。场地间距宽敞，跑动完全不会影响邻场，每小时50块的性价比真的没谁了，强烈推荐打双打！",
                    "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：感谢球友推荐！2号场专门拓宽了外沿缓冲区，给双打大范围跑位留足了空间，性价比常青树！",
                    31, LocalDateTime.now().minusDays(1).minusHours(6)
                },
                {
                    10005L, 2L, 5L, 5, 5, 4, 5,
                    "场地整洁,保洁阿姨勤快,平价功能饮品",
                    "场地上一点灰尘都没有，保洁阿姨每隔一个小时就用专业静电拖把推尘，地胶抓地感很踏实。前台自动售货机佳得乐和宝矿力价格跟超市一样良心，必须点赞！",
                    "",
                    "【店长回复】：卫生第一，细节致胜！平价冷饮和整洁环境是我们的基本原则，欢迎常来！",
                    15, LocalDateTime.now().minusDays(4).minusHours(2)
                },

                // 场馆 3: 羽毛球 3 号场 (进阶训练场)
                {
                    10006L, 3L, 5L, 5, 5, 5, 5,
                    "录像战术支架,多机位复盘,训练氛围拉满,发球机好用",
                    "球场边居然自带多角度手机训练录像支架，还有战术小白板！自己架上手机录了正手起跳劈吊和滑板吊球，回去一帧一帧复盘，球技提升太快了。前台还可以租借全自动发球机！",
                    "https://images.unsplash.com/photo-1544919982-b61976f0ba43?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：太懂球了！3号场就是为想要突破瓶颈期的进阶球友量身打造的'实验室'，欢迎多来录制高光集锦！",
                    35, LocalDateTime.now().minusDays(2).minusHours(8)
                },
                {
                    10007L, 3L, 2L, 5, 4, 5, 5,
                    "青训安全推荐,多层复合减震,保护膝盖",
                    "带队里几个小队员集训，地胶下层做了多层复合龙骨减震，连续练两个小时步伐脚底板和跟腱都不酸，安全系数很高，家长们看了也很满意。",
                    "",
                    "【店长回复】：科学运动，保护运动寿命！我们的弹性减震经过专业机构认证，期待更多小冠军从这里走出！",
                    20, LocalDateTime.now().minusDays(5).minusHours(4)
                },

                // 场馆 4: 中心网球 1 号场 (红土体验)
                {
                    10008L, 4L, 1L, 5, 5, 5, 5,
                    "法网红土同款,滑步极致丝滑,高亮度夜场泛光,尊贵仪式感",
                    "全市找不出第二家这么正宗的红土球场！颗粒均匀细致，法网红土同款滑步手感让人上瘾，急停滑行卸力极其自然。晚上八盏大功率无频闪泛光灯一开，比白天视线还清晰，尊崇感直接拉满！",
                    "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：感谢网球资深爱好者的最高嘉奖！我们特意采购纯正进口天然红砖陶粒粉，每日赛后雾化喷淋养护并专业平整，为您保留最醇厚的罗兰加洛斯红土质感！",
                    58, LocalDateTime.now().minusDays(1).minusHours(2)
                },
                {
                    10009L, 4L, 5L, 5, 5, 5, 5,
                    "五星级服务,提供专用扫线器,温热毛巾,球童贴心",
                    "跟私教练发球和底线正反手相持，打完后工作人员贴心送上冰镇苏打水和热毛巾，还主动帮忙整理球框。换衣洗漱出来神清气爽，这120块一小时花得太值了！",
                    "",
                    "【店长回复】：细致关怀是我们的标准流程。红土球场不仅是运动场，更是尊享社交空间，期待再次接待您！",
                    29, LocalDateTime.now().minusDays(3).minusHours(6)
                },

                // 场馆 5: 室内篮球半场 A (木地板)
                {
                    10010L, 5L, 2L, 5, 5, 5, 5,
                    "NBA级实木地板,急停变向不打滑,篮网声音清脆,空调劲爆",
                    "美职篮同款北美枫木双层龙骨地板，落地完全没有生硬反震，鞋底摩擦声音超级清脆吱吱叫！篮圈弹性刚刚好，链网加织网进球声音太治愈了，全场空调给力，夏天打球居然还会觉得凉快！",
                    "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1519766304817-4f37bda74a29?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：听到空心入网那声脆响就是球场最美的乐章！我们地板每月定期打专业防滑耐磨养护蜡，欢迎带兄弟们常来约战！",
                    47, LocalDateTime.now().minusDays(1).minusHours(4)
                },
                {
                    10011L, 5L, 5L, 5, 5, 5, 5,
                    "独立新风充足,手机蓝牙投屏计分,无盲区抓拍",
                    "我们部门五打五半场争霸赛，LED电子大屏可以直接连手机蓝牙计分、放队歌！全馆新风系统换气速度极快，打到最后大家一点都没觉得缺氧，气氛燃爆了！",
                    "",
                    "【店长回复】：团建选这里太有眼光了！智能计分大屏与大风量新风正是为热血赛事定制，期待贵司下次再办联赛！",
                    33, LocalDateTime.now().minusDays(4).minusHours(5)
                },

                // 场馆 6: 云端多媒体会议室 (20人)
                {
                    10012L, 6L, 1L, 5, 5, 5, 5,
                    "4K高清双屏,静音隔音极佳,千兆专线稳定,现磨咖啡香醇",
                    "公司季度高管战略闭门研讨会选在这里，效果超出预期！双屏4K激光投影无缝分屏展示财务报表与代码架构，无线投屏丝毫没有卡顿。双层真空隔音玻璃，门一关外面鸦雀无声，私密性极佳！",
                    "https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：感谢战略客户的高规格信赖！云端会议空间专为高端商务、投融资路演与闭门研讨量身设计，配备千兆独立光纤与智能静音系统，助您商战捷报频传！",
                    39, LocalDateTime.now().minusDays(2).minusHours(7)
                },
                {
                    10013L, 6L, 5L, 5, 5, 5, 5,
                    "人体工学皮椅,智能电子白板,茶歇精致,前台体面",
                    "用来组织了一场技术沙龙交流，交互式电子白板直接可以扫码把板书笔记带走，椅子是赫曼米勒同款工学椅，连续坐了四个小时腰一点不酸，参会嘉宾纷纷夸赞场地规格高级。",
                    "",
                    "【店长回复】：能够为高水平技术沙龙赋能是我们的荣幸！电子白板一键扫码存云端就是为了无纸化高效协作，期待常来！",
                    24, LocalDateTime.now().minusDays(3).minusHours(9)
                },

                // 场馆 7: 奥体国际标准恒温泳池 (50米8泳道)
                {
                    10014L, 7L, 5L, 5, 5, 5, 5,
                    "恒温水质清澈,臭氧消毒无氯味,救生员专业负责,淋浴水流强劲",
                    "水质非常清亮见底，完全没有传统泳池刺鼻的氯气味！50米大池游起来太舒展了，水温常年28度很舒适。淋浴间水压超足，更衣柜电子感应很方便。",
                    "https://images.unsplash.com/photo-1576610616656-d3aa5d1f4534?w=800&auto=format&fit=crop&q=60,https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：感谢泳友的高度评价！我们采用德国逆流式循环与臭氧辅以硅藻土物理过滤，全天候保持国际泳联竞赛级水质标准，欢迎常来畅游！",
                    45, LocalDateTime.now().minusDays(1).minusHours(1)
                },
                {
                    10015L, 7L, 2L, 5, 5, 5, 5,
                    "泳道宽敞不挤,深水区视线清晰,智能手环储物",
                    "深水区划线非常醒目，救生员一直来回巡视安全感十足。晚上来游人也不挤，一人一个泳道太爽了，游完在休息区喝一杯电解质水特别舒服。",
                    "",
                    "【店长回复】：安全与畅快是我们的核心承诺！泳道分流管控保证每位泳者拥有独立舒适的行进空间，期待下次光临！",
                    22, LocalDateTime.now().minusDays(3).minusHours(4)
                },

                // 场馆 8: 亲子水适能与康复浅水池
                {
                    10016L, 8L, 5L, 5, 5, 5, 5,
                    "32℃温水舒适,儿童浮水教具齐全,亲子独立更衣室贴心",
                    "带5岁宝宝来上启蒙水适能课，水温32度很温暖，完全不用担心孩子着凉！防滑地垫铺得很满，还提供各种可爱的浮力背心和水上玩具，宝宝玩得超级开心！",
                    "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：看到小朋友玩得开心就是我们最欣慰的事！亲子水域专设阶梯式缓坡水深与恒定32℃暖水，呵护孩子每一步水上成长！",
                    38, LocalDateTime.now().minusDays(2).minusHours(3)
                },
                {
                    10017L, 8L, 2L, 5, 5, 5, 5,
                    "水中康复舒缓,无障碍坡道友好,教练耐心细致",
                    "术后做膝关节水中抗阻康复训练，水深刚好到胸口，浮力卸掉身体重量后走动完全不痛。教练很专业，全程指导姿势，环境安静不嘈杂。",
                    "",
                    "【店长回复】：祝您身体早日康复如初！浅水池专为低负荷水动能康复设计，我们会持续为您提供细致温和的环境！",
                    19, LocalDateTime.now().minusDays(4).minusHours(6)
                },

                // 场馆 9: 乒乓球 1 号专业比赛场 (红双喜彩虹台)
                {
                    10018L, 9L, 2L, 5, 5, 5, 5,
                    "红双喜金彩虹台,红地胶抓地稳,发球机练球神仙搭配",
                    "球台弹性均匀无死角，台面磨砂摩擦力极好，拉弧圈球旋转强烈！地胶踩上去很弹很稳。前台租的发球机可以自编旋转和落点，自己一个人练了俩小时正反手衔接，大汗淋漓！",
                    "https://images.unsplash.com/photo-1534158914592-062992fbe900?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：老球手一上手就知道分量！国家队世乒赛同款彩虹台搭配智能发球机，就是为独行侠球友打造的练级圣地，欢迎常来切磋！",
                    33, LocalDateTime.now().minusDays(1).minusHours(5)
                },
                {
                    10019L, 9L, 5L, 5, 5, 4, 5,
                    "防眩漫反射顶灯,独立战术观战椅,性价比极高",
                    "灯光设计很懂乒乓球，漫反射完全不晃眼睛，打高球也不会丢失视野。边上有专门放毛巾和水壶的置物架，35块钱一小时在市区简直是神仙价格。",
                    "",
                    "【店长回复】：乒乓国球就该全民普惠！专业级照明与高性价比是我们对球友的长情陪伴，感谢认可！",
                    25, LocalDateTime.now().minusDays(3).minusHours(2)
                },

                // 场馆 10: 乔氏中式黑八尊享包厢 (金腿球台)
                {
                    10020L, 10L, 1L, 5, 5, 5, 5,
                    "乔氏金腿石板极平整,英国6811台呢走球丝滑,雅乐美比赛球",
                    "台球发烧友必来！乔氏金腿真材实料，石板调校得分毫不差，慢速库边反弹角度精准无比。雅乐美TV比赛球撞击声音清脆通透，独立包厢带新风抽风机，完全没异味！",
                    "https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：遇上行家里手了！英国进口6811台呢与高精度温控石板每周定期精密调平，给每一杆走位最细腻的物理反馈！",
                    52, LocalDateTime.now().minusDays(2).minusHours(6)
                },
                {
                    10021L, 10L, 5L, 5, 5, 5, 5,
                    "商务接待有面子,皮质沙发茶歇舒适,现磨咖啡正宗",
                    "请客户来打台球谈业务，包厢私密性极好，门一关静音效果出色。前台送来的现磨美式咖啡很纯正，沙发坐着很舒服，客户赞不绝口，合同也顺利推进了！",
                    "",
                    "【店长回复】：恭喜老板业务长虹！台球与商务社交在此完美融合，竭诚为您打造高品位会客体验！",
                    28, LocalDateTime.now().minusDays(5).minusHours(3)
                },

                // 场馆 11: 潮流匹克球 1 号场 (低冲击高弹硬地)
                {
                    10022L, 11L, 5L, 5, 5, 5, 5,
                    "匹克球上手极快,免费借碳纤维球拍,动感音乐气氛超嗨",
                    "最近全网爆火的匹克球终于体验到了！零基础小白5分钟就学会了发球和截击，比网球容易上手，比羽毛球更适合社交。前台小哥免费借了碳纤维球拍，还送了吸汗带，太暖心了！",
                    "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：匹克球的快乐就是这么直接！低门槛、高热量消耗和欢乐社交，下周五还有匹克球单身盲盒切磋夜，期待您参与！",
                    41, LocalDateTime.now().minusDays(1).minusHours(7)
                },
                {
                    10023L, 11L, 2L, 5, 5, 5, 4,
                    "丙烯酸面层减震好,室内空调给力,聚会团建不二之选",
                    "跟几个大学同学聚会选在这，球场画线清晰，场地回弹适中对脚踝很友好。全场冷气开得很足，连续打两小时一点不闷，已经约了下周继续包场！",
                    "",
                    "【店长回复】：青春就是挥汗如雨的欢笑！丙烯酸多层缓冲面层专门减轻了半月板负荷，祝你们友谊长青！",
                    17, LocalDateTime.now().minusDays(3).minusHours(8)
                },

                // 场馆 12: 德国 ASB 全透玻璃壁球馆
                {
                    10024L, 12L, 1L, 5, 5, 5, 5,
                    "全透钢化玻璃视觉震撼,ASB前墙击球回音超爽,减压天花板",
                    "下班后极度解压！全透明玻璃设计科技感爆棚，壁球打在ASB墙上那声‘咚’的闷响太让人上瘾了！40分钟心率飙到160，暴汗排毒，浑身通透！",
                    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：高压都市生活最好的情绪解药！德国原装ASB专业回音弹性墙体，让每一次挥拍击球都成为力量的彻底释放！",
                    49, LocalDateTime.now().minusDays(1).minusHours(9)
                },
                {
                    10025L, 12L, 5L, 5, 5, 5, 5,
                    "枫木抗震地板很弹,更衣淋浴干净,备有护目镜很专业",
                    "新手第一次打壁球，前台非常负责地强调必须佩戴护目镜并免费提供。抗震地板落地很轻盈，不会震得脑仁疼，洗澡水温水压很赞！",
                    "",
                    "【店长回复】：安全是竞技的第一准则！专业护目镜与高弹地板为您的极速变向提供周密防护，期待常来！",
                    26, LocalDateTime.now().minusDays(4).minusHours(4)
                },

                // 场馆 13: 光影普拉提大器械核心床私教房
                {
                    10026L, 13L, 5L, 5, 5, 5, 5,
                    "进口Reformer核心床丝滑,原木风光影超美,私密安静出片",
                    "环境美得像艺术馆！下午阳光斜照进来光影绝美，随手一拍发小红书赞爆。进口核心床滑轨顺滑无声，弹簧阻力细腻均匀，做百次拍打和卷腹时核心激活特别深！",
                    "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：普拉提是身体与心灵的建筑学。静谧的原木自然光影搭配顶级核心床器械，让每一次呼吸与延展都成为享受！",
                    63, LocalDateTime.now().minusDays(1).minusHours(4)
                },
                {
                    10027L, 13L, 2L, 5, 5, 5, 5,
                    "凯迪拉克秋千架齐全,脊柱拉伸改善体态,空气新风清新",
                    "长期久坐腰肌劳损，来做了针对骨盆后倾和胸椎灵活性训练。凯迪拉克床配件很全，练完拉伸下来感觉整个人长高了2厘米！独立房间非常私密，没有异味。",
                    "",
                    "【店长回复】：改善体态、赋能活力正是普拉提的核心魅力！坚持练习，身体会给您最惊喜的正向反馈！",
                    31, LocalDateTime.now().minusDays(3).minusHours(5)
                },

                // 场馆 14: 绿茵天地五人制室内笼式足球场
                {
                    10028L, 14L, 2L, 5, 5, 5, 5,
                    "免充砂环保草皮脚感极软,无盲区LED大灯,围网结实防飞球",
                    "周五晚上公司足球俱乐部约战！免充砂环保草皮脚感太舒服了，鞋钉抓地牢固，铲球完全不划破皮。全包围网非常结实，踢飞的球瞬间回弹，比赛节奏极快！",
                    "https://images.unsplash.com/photo-1529900241467-172608493132?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：无热血，不足球！免充砂草皮专为室内快节奏短传配合设计，环保无异味防擦伤，欢迎常来踢夜场！",
                    56, LocalDateTime.now().minusDays(2).minusHours(5)
                },
                {
                    10029L, 14L, 5L, 5, 5, 5, 5,
                    "更衣室配大储物柜,冷饮充足,组织比赛很省心",
                    "组织了一场校友五人制对抗赛，球场自带电子计分板和替换背心。踢完大家冲个热水澡神清气爽，停车也很方便，大家都说下次还定这里！",
                    "",
                    "【店长回复】：校友欢聚，球场添彩！我们提供全套赛事物料支持与便捷泊车，期待为您的下一次联赛服务！",
                    27, LocalDateTime.now().minusDays(4).minusHours(7)
                },

                // 场馆 15: 中心网球 2 号场 (澳网同款快速硬地)
                {
                    10030L, 15L, 1L, 5, 5, 5, 5,
                    "澳网经典蓝硬地,球速快弹跳规整,发球测速雷达高级",
                    "纯正澳网同款硬地！球弹起来非常规整，没有死角怪弹，底线发力击球的声音像炮弹一样带感。场边还架设了发球测速雷达，今天一发测到178km/h，打得超兴奋！",
                    "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：178km/h一发堪比职业水准！澳网蓝快速硬地面层与专业测速雷达就是为硬核竞技而生，期待您刷新个人时速纪录！",
                    44, LocalDateTime.now().minusDays(2).minusHours(3)
                },
                {
                    10031L, 15L, 5L, 5, 5, 4, 5,
                    "夜场泛光无阴影,球网标准紧挺,休息长椅干净",
                    "晚上和教练练底线截击，大功率高悬照明完全照亮全场，没有视野死角。球场排水坡度做得很好，地面干燥防滑，体验挑不出毛病。",
                    "",
                    "【店长回复】：专注细节，精益求精！专业级照明与严谨平整度保障您的每一次发球上网，祝球技精进！",
                    21, LocalDateTime.now().minusDays(5).minusHours(2)
                },

                // 场馆 16: 室内篮球赛事级全场 (奥运木地板)
                {
                    10032L, 16L, 2L, 5, 5, 5, 5,
                    "国际篮联双龙骨枫木,液压篮架极稳固,4K大屏比赛氛围超燃",
                    "行业邀请赛总决赛定在这里办，真的有打NBA的感觉！双龙骨枫木地板弹性极佳，落地膝盖毫无冲击感。扣篮挂筐液压篮架纹丝不动，LED大屏滚动放比分和集锦，观众席坐满呼声雷动！",
                    "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60",
                    "【店长回复】：热血澎湃的经典对决！赛事级实木全场与液压竞赛篮架专为高强度对抗而生，恭喜冠军战队，期待更多辉煌赛事在此诞生！",
                    68, LocalDateTime.now().minusDays(1).minusHours(5)
                },
                {
                    10033L, 16L, 5L, 5, 5, 5, 5,
                    "中央空调全馆凉爽,更衣室淋浴喷头水量超大,安保专业体面",
                    "包全场打了两小时全场对抗，全馆中央空调完全顶得住二十个人的热量，一点都不闷热。打完洗澡水温恒定，喷头水量大，出来前台还主动帮忙开电子发票，正规体面！",
                    "",
                    "【店长回复】：专业保障，用心护航！大制冷量中央空调与高标准卫浴系统全力护航赛事与团建，期待常来！",
                    35, LocalDateTime.now().minusDays(3).minusHours(4)
                }
            };

            for (Object[] r : reviews) {
                if (Long.valueOf(5L).equals(r[2])) {
                    r[2] = user1Id;
                }
                jdbcTemplate.update(insertSql, r);
            }
            log.info("数据库初始化: 全量场馆 (1-16号) 美团团购风真实口碑评价初始化完成 (共导入 {} 条精美点评)", reviews.length);
        } catch (Exception e) {
            log.warn("初始化美团式场馆评价数据异常: {}", e.getMessage());
        }
    }
}
