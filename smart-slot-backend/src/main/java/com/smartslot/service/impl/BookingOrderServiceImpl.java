package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.BookingCreateDto;
import com.smartslot.dto.ReviewCreateDto;
import com.smartslot.dto.SlotEventDto;
import com.smartslot.entity.*;
import com.smartslot.mapper.*;
import com.smartslot.service.BookingOrderService;
import com.smartslot.service.CouponService;
import com.smartslot.service.IotGateService;
import com.smartslot.service.OrderDelayQueueService;
import com.smartslot.service.WebSocketPushService;
import com.smartslot.util.LuaLockManager;
import com.smartslot.vo.SlotMatrixVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约核心服务实现类
 * 覆盖：Lua原子时段锁防超卖、Redisson延时队列关单、MyBatis-Plus 分页、组件化矩阵数据源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOrderServiceImpl extends ServiceImpl<BookingOrderMapper, BookingOrder> implements BookingOrderService {

    private final VenueMapper venueMapper;
    private final VenueCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final OrderReviewMapper reviewMapper;
    private final LuaLockManager luaLockManager;
    private final OrderDelayQueueService orderDelayQueueService;
    private final WebSocketPushService webSocketPushService;
    private final IotGateService iotGateService;
    private final CouponService couponService;

    private static final List<String> STANDARD_SLOTS = List.of(
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00",
            "17:00-18:00", "18:00-19:00", "19:00-20:00", "20:00-21:00",
            "21:00-22:00"
    );

    @Override
    public SlotMatrixVo getSlotMatrix(LocalDate bookDate, Long categoryId, Long currentUserId) {
        if (bookDate == null) {
            bookDate = LocalDate.now();
        }

        // 1. 查询指定分类或全部的有效场地
        List<Venue> venues = venueMapper.selectList(new LambdaQueryWrapper<Venue>()
                .eq(categoryId != null, Venue::getCategoryId, categoryId)
                .orderByAsc(Venue::getId));

        // 2. 查询当天所有非取消状态的订单
        List<BookingOrder> dayOrders = list(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getBookDate, bookDate)
                .in(BookingOrder::getOrderStatus, 0, 1, 2));

        // 以 "venueId:timeSlot" 为 Key 建立映射
        Map<String, BookingOrder> orderMap = dayOrders.stream()
                .collect(Collectors.toMap(
                        o -> o.getVenueId() + ":" + o.getTimeSlot(),
                        o -> o,
                        (k1, k2) -> k1
                ));

        // 3. 构建矩阵列
        List<SlotMatrixVo.VenueColumnVo> venueColumns = new ArrayList<>();
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(VenueCategory::getId, VenueCategory::getName, (k1, k2) -> k1));

        LocalDateTime now = LocalDateTime.now();

        for (Venue venue : venues) {
            List<SlotMatrixVo.SlotItemVo> slotItems = new ArrayList<>();
            boolean isMaintenance = (venue.getStatus() == 0);

            for (String slot : STANDARD_SLOTS) {
                String slotKey = venue.getId() + ":" + slot;
                BookingOrder order = orderMap.get(slotKey);
                String lockKey = buildLockKey(venue.getId(), bookDate, slot);

                int status = 0; // 默认空闲
                boolean isMine = false;
                String orderNo = null;
                String verifyCode = null;

                if (isMaintenance) {
                    status = 3; // 场地维护
                } else if (order != null) {
                    if (order.getOrderStatus() == 0) {
                        // 待支付状态，检查是否已过 15 分钟超时时间
                        if (order.getExpireTime() != null && order.getExpireTime().isBefore(now)) {
                            status = 0; // 超时自动失效，恢复空闲
                            luaLockManager.unlockAtomic(lockKey, "FORCE_UNLOCK");
                        } else {
                            status = 1; // 待支付锁定中
                            if (currentUserId != null && currentUserId.equals(order.getUserId())) {
                                isMine = true;
                                orderNo = order.getOrderNo();
                            }
                        }
                    } else if (order.getOrderStatus() == 1 || order.getOrderStatus() == 2) {
                        status = 2; // 已预约成功或已核销
                        if (currentUserId != null && currentUserId.equals(order.getUserId())) {
                            isMine = true;
                            orderNo = order.getOrderNo();
                            verifyCode = order.getVerifyCode();
                        }
                    }
                } else if (luaLockManager.isLocked(lockKey)) {
                    status = 1; // 内存/Redis 临时锁占用中
                }

                slotItems.add(SlotMatrixVo.SlotItemVo.builder()
                        .timeSlot(slot)
                        .status(status)
                        .isMine(isMine)
                        .orderNo(orderNo)
                        .verifyCode(verifyCode)
                        .build());
            }

            venueColumns.add(SlotMatrixVo.VenueColumnVo.builder()
                    .venueId(venue.getId())
                    .venueName(venue.getName())
                    .categoryName(categoryMap.getOrDefault(venue.getCategoryId(), "通用场馆"))
                    .pricePerHour(venue.getPricePerHour())
                    .venueStatus(venue.getStatus())
                    .slots(slotItems)
                    .build());
        }

        return SlotMatrixVo.builder()
                .date(bookDate.toString())
                .timeSlots(STANDARD_SLOTS)
                .venues(venueColumns)
                .build();
    }

    /**
     * 核心亮点：时段预占与防超卖
     * 1. 尝试使用 Redis / 本地分布式锁原子锁定 15 分钟
     * 2. 数据库落库待支付订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingOrder lockAndCreateOrder(BookingCreateDto dto, Long userId) {
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null || venue.getStatus() == 0) {
            throw new BusinessException("该场地暂未开放或正在维护中");
        }

        if (dto.getBookDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约过去的日期");
        }

        String lockKey = buildLockKey(dto.getVenueId(), dto.getBookDate(), dto.getTimeSlot());

        // 1. 尝试使用 Lua 脚本原子加锁 15 分钟 (900秒)
        String lockVal = "UID:" + userId;
        boolean lockSuccess = luaLockManager.tryLockAtomic(lockKey, lockVal, 900);
        if (!lockSuccess) {
            throw new BusinessException("该时段刚刚被他人抢先锁定，请选择其他时段");
        }

        // 2. 双重检查数据库中是否存在有效订单
        BookingOrder existingOrder = getOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getVenueId, dto.getVenueId())
                .eq(BookingOrder::getBookDate, dto.getBookDate())
                .eq(BookingOrder::getTimeSlot, dto.getTimeSlot())
                .in(BookingOrder::getOrderStatus, 0, 1, 2)
                .last("LIMIT 1"));

        if (existingOrder != null) {
            // 如果存在待支付且未超时的，则已被占
            if (existingOrder.getOrderStatus() == 0 && existingOrder.getExpireTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException("该时段已被预约锁定，请选择其他时段");
            }
            if (existingOrder.getOrderStatus() == 1 || existingOrder.getOrderStatus() == 2) {
                throw new BusinessException("该时段已售出，无法重复预约");
            }
        }

        // 3. 生成业务订单号与优惠券试算抵扣
        String orderNo = "ORD" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
        LocalDateTime now = LocalDateTime.now();

        BigDecimal originalAmount = venue.getPricePerHour();
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (dto.getUserCouponId() != null) {
            discountAmount = couponService.consumeUserCoupon(dto.getUserCouponId(), userId, originalAmount, orderNo);
        }
        BigDecimal actualAmount = originalAmount.subtract(discountAmount);
        if (actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            actualAmount = new BigDecimal("0.01");
            discountAmount = originalAmount.subtract(actualAmount);
        }

        BookingOrder order = BookingOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .venueId(dto.getVenueId())
                .bookDate(dto.getBookDate())
                .timeSlot(dto.getTimeSlot())
                .totalAmount(actualAmount) // 实付需支付金额
                .couponId(dto.getUserCouponId())
                .discountAmount(discountAmount)
                .actualAmount(actualAmount)
                .payStatus(0) // 未支付
                .orderStatus(0) // 待支付锁定中
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .expireTime(now.plusMinutes(15))
                .createTime(now)
                .updateTime(now)
                .build();

        save(order);

        // 4. 投递 Redisson / JVM 延时队列，15 分钟未支付自动关单释放库存
        orderDelayQueueService.sendOrderTimeoutDelay(orderNo, 15, TimeUnit.MINUTES);

        // 5. WebSocket 全网毫秒级广播：通知所有在线用户该时段已被锁定
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("LOCK")
                .venueId(dto.getVenueId())
                .venueName(venue.getName())
                .bookDate(dto.getBookDate())
                .timeSlot(dto.getTimeSlot())
                .status(1)
                .userId(userId)
                .message("场地【" + venue.getName() + "】时段 " + dto.getTimeSlot() + " 刚被抢先锁定")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("成功锁定并创建预约订单: orderNo={}, userId={}, venueId={}, timeSlot={}",
                orderNo, userId, dto.getVenueId(), dto.getTimeSlot());
        return order;
    }

    /**
     * 模拟支付完成
     * 扣减余额、生成6位核销码、修改订单状态为已预约
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingOrder payOrder(String orderNo, Long userId) {
        BookingOrder order = getOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人订单");
        }

        if (order.getOrderStatus() != 0) {
            throw new BusinessException("订单状态不支持支付");
        }

        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setOrderStatus(3); // 超时自动取消
            order.setCancelReason("支付超时自动取消");
            updateById(order);
            luaLockManager.unlockAtomic(buildLockKey(order.getVenueId(), order.getBookDate(), order.getTimeSlot()), "FORCE_UNLOCK");
            throw new BusinessException("订单已过 15 分钟支付期限，已被释放");
        }

        SysUser user = userMapper.selectById(userId);
        if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new BusinessException("账户余额不足 (当前余额: ￥" + user.getBalance() + ")，请联系客服充值");
        }

        // 扣减用户虚拟余额
        user.setBalance(user.getBalance().subtract(order.getTotalAmount()));
        userMapper.updateById(user);

        // 生成唯一 6 位数字专属核销码
        String verifyCode = String.format("%06d", new Random().nextInt(900000) + 100000);

        order.setPayStatus(1); // 已支付
        order.setOrderStatus(1); // 预约成功(待核销)
        order.setPayTime(LocalDateTime.now());
        order.setVerifyCode(verifyCode);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 广播时段已完成支付出票
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("PAY")
                .venueId(order.getVenueId())
                .bookDate(order.getBookDate())
                .timeSlot(order.getTimeSlot())
                .status(2)
                .userId(userId)
                .message("时段 " + order.getTimeSlot() + " 已出票成功")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("订单支付成功: orderNo={}, 核销码={}", orderNo, verifyCode);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, String reason) {
        BookingOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权取消他人订单");
        }

        if (order.getOrderStatus() == 2) {
            throw new BusinessException("订单已核销使用，无法取消");
        }

        if (order.getOrderStatus() == 3) {
            throw new BusinessException("订单已处于取消状态");
        }

        // 如果已支付，原路退回用户余额
        if (order.getPayStatus() == 1) {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                user.setBalance(user.getBalance().add(order.getTotalAmount()));
                userMapper.updateById(user);
            }
            order.setPayStatus(2); // 已退款
        }

        order.setOrderStatus(3); // 已取消
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "用户自主取消");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 如果使用了优惠券，退还优惠券
        couponService.rollbackUserCoupon(order.getOrderNo());

        // 释放时段锁
        luaLockManager.unlockAtomic(buildLockKey(order.getVenueId(), order.getBookDate(), order.getTimeSlot()), "FORCE_UNLOCK");

        // 广播时段已取消释放
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("CANCEL")
                .venueId(order.getVenueId())
                .bookDate(order.getBookDate())
                .timeSlot(order.getTimeSlot())
                .status(0)
                .userId(userId)
                .message("时段 " + order.getTimeSlot() + " 已取消并恢复空闲")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("订单已成功取消并释放时段: orderNo={}", order.getOrderNo());
    }

    /**
     * 管理端输入 6 位核销码核验入场
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingOrder verifyOrder(String verifyCode) {
        if (!StringUtils.hasText(verifyCode)) {
            throw new BusinessException("请输入核销码");
        }

        BookingOrder order = getOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getVerifyCode, verifyCode.trim()));

        if (order == null) {
            throw new BusinessException("核销码不存在或无效");
        }

        if (order.getOrderStatus() == 2) {
            throw new BusinessException("该订单已于 " + order.getVerifyTime() + " 完成核销，请勿重复核验");
        }

        if (order.getOrderStatus() != 1) {
            throw new BusinessException("该订单当前状态不可核销 (状态码: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(2); // 已核销
        order.setVerifyTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        Venue venue = venueMapper.selectById(order.getVenueId());
        if (venue != null) {
            order.setVenueName(venue.getName());
        }

        // 联动智能物联网门禁与道闸：下发开闸放行 MQTT 指令
        try {
            iotGateService.sendGateOpenCommand(order);
        } catch (Exception e) {
            log.warn("[IoT道闸] 开闸指令下发异常: {}", e.getMessage());
        }

        log.info("核销成功: orderNo={}, verifyCode={}", order.getOrderNo(), verifyCode);
        return order;
    }

    /**
     * 用户端订单分页 (MyBatis-Plus 分页插件)
     */
    @Override
    public Page<BookingOrder> pageUserOrders(Page<BookingOrder> page, Long userId, Integer status) {
        LambdaQueryWrapper<BookingOrder> wrapper = new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId)
                .eq(status != null, BookingOrder::getOrderStatus, status)
                .orderByDesc(BookingOrder::getId);

        Page<BookingOrder> result = page(page, wrapper);
        fillOrderDetails(result.getRecords());
        return result;
    }

    /**
     * 管理端订单分页 (MyBatis-Plus 分页插件)
     */
    @Override
    public Page<BookingOrder> pageAdminOrders(Page<BookingOrder> page, String orderNo, String phone, Integer status) {
        LambdaQueryWrapper<BookingOrder> wrapper = new LambdaQueryWrapper<BookingOrder>()
                .like(StringUtils.hasText(orderNo), BookingOrder::getOrderNo, orderNo)
                .like(StringUtils.hasText(phone), BookingOrder::getContactPhone, phone)
                .eq(status != null, BookingOrder::getOrderStatus, status)
                .orderByDesc(BookingOrder::getId);

        Page<BookingOrder> result = page(page, wrapper);
        fillOrderDetails(result.getRecords());
        return result;
    }

    @Override
    public void addReview(ReviewCreateDto dto, Long userId) {
        Long targetVenueId = dto.getVenueId();
        Long orderId = dto.getOrderId();

        if (orderId != null && orderId > 0) {
            BookingOrder order = getById(orderId);
            if (order != null) {
                if (!order.getUserId().equals(userId)) {
                    throw new BusinessException("只能评价自己的订单");
                }
                if (order.getOrderStatus() != 2) {
                    throw new BusinessException("只有已核销完成的订单才能进行评价");
                }
                targetVenueId = order.getVenueId();
                OrderReview existing = reviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getOrderId, orderId));
                if (existing != null) {
                    throw new BusinessException("该订单已评价，请勿重复提交");
                }
            }
        } else {
            orderId = System.currentTimeMillis();
        }

        if (targetVenueId == null) {
            targetVenueId = 1L;
        }

        OrderReview review = OrderReview.builder()
                .orderId(orderId)
                .venueId(targetVenueId)
                .userId(userId)
                .rating(dto.getRating())
                .envRating(dto.getEnvRating() != null ? dto.getEnvRating() : dto.getRating())
                .facilityRating(dto.getFacilityRating() != null ? dto.getFacilityRating() : dto.getRating())
                .serviceRating(dto.getServiceRating() != null ? dto.getServiceRating() : dto.getRating())
                .tags(dto.getTags() != null ? dto.getTags() : "")
                .images(dto.getImages() != null ? dto.getImages() : "")
                .merchantReply("【店长回复】：感谢球友的热情支持与真实反馈！我们将持续精进场地设施与防滑维护，期待与您在球场再次相见！")
                .likes(0)
                .content(dto.getContent())
                .createTime(LocalDateTime.now())
                .build();

        reviewMapper.insert(review);
    }

    /**
     * Redisson 延迟队列消费者自动触发的超时订单关单与时段锁释放
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeoutOrder(String orderNo) {
        BookingOrder order = getOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderNo, orderNo));
        if (order == null) {
            log.warn("[延时关单] 订单不存在: orderNo={}", orderNo);
            return;
        }

        // 仅待支付状态(orderStatus == 0)执行超时自动关单与时段释放
        if (order.getOrderStatus() == 0) {
            order.setOrderStatus(3); // 3: 已取消
            order.setCancelReason("支付超时(15分钟)，系统自动关闭订单并释放锁");
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);

            // 如果使用了优惠券，退还优惠券
            couponService.rollbackUserCoupon(order.getOrderNo());

            String lockKey = buildLockKey(order.getVenueId(), order.getBookDate(), order.getTimeSlot());
            luaLockManager.unlockAtomic(lockKey, "FORCE_UNLOCK");

            // WebSocket 全网广播：超时释放通知
            webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                    .eventType("TIMEOUT")
                    .venueId(order.getVenueId())
                    .bookDate(order.getBookDate())
                    .timeSlot(order.getTimeSlot())
                    .status(0)
                    .userId(order.getUserId())
                    .message("时段 " + order.getTimeSlot() + " 支付超时，已由延迟队列自动释放")
                    .timestamp(System.currentTimeMillis())
                    .build());

            log.info("[延时关单] 订单超时成功关单并释放时段锁: orderNo={}, lockKey={}", orderNo, lockKey);
        } else {
            log.info("[延时关单] 订单非待支付状态，无需关单: orderNo={}, status={}", orderNo, order.getOrderStatus());
        }
    }

    /**
     * 全链路对账与时段状态自愈
     * 自动扫描数据库中逾期但因极端网络抖动未关闭的超时锁定订单，闭环回补释放时段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reconcileExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingOrder> expiredOrders = list(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderStatus, 0)
                .le(BookingOrder::getExpireTime, now));

        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return 0;
        }

        int healedCount = 0;
        for (BookingOrder order : expiredOrders) {
            try {
                handleTimeoutOrder(order.getOrderNo());
                healedCount++;
            } catch (Exception e) {
                log.error("[对账自愈] 处理超时订单异常: orderNo={}", order.getOrderNo(), e);
            }
        }
        if (healedCount > 0) {
            log.info("[对账自愈] 自动巡检自愈完成，成功修复并回补释放超时时段 {} 个", healedCount);
        }
        return healedCount;
    }

    private void fillOrderDetails(List<BookingOrder> orders) {
        if (orders.isEmpty()) return;
        List<Venue> venues = venueMapper.selectList(null);
        Map<Long, String> venueMap = venues.stream().collect(Collectors.toMap(Venue::getId, Venue::getName, (k1, k2) -> k1));
        orders.forEach(o -> o.setVenueName(venueMap.getOrDefault(o.getVenueId(), "指定场地")));
    }

    private String buildLockKey(Long venueId, LocalDate bookDate, String timeSlot) {
        return String.format("slot:lock:%d:%s:%s", venueId, bookDate, timeSlot);
    }
}
