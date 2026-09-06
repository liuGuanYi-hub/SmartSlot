package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.PrepayRequestDto;
import com.smartslot.dto.PrepayResponseDto;
import com.smartslot.dto.SlotEventDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.PaymentRecord;
import com.smartslot.entity.SysUser;
import com.smartslot.entity.Venue;
import com.smartslot.mapper.BookingOrderMapper;
import com.smartslot.mapper.PaymentRecordMapper;
import com.smartslot.mapper.SysUserMapper;
import com.smartslot.mapper.VenueMapper;
import com.smartslot.service.PaymentGatewayService;
import com.smartslot.service.WebSocketPushService;
import com.smartslot.vo.PaymentReconciliationVo;
import com.smartslot.vo.ReconciliationSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentGatewayService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final VenueMapper venueMapper;
    private final WebSocketPushService webSocketPushService;
    private final ObjectMapper objectMapper;

    private static final String ALIPAY_SANDBOX_APPID = "2021000118654321";
    private static final String WECHAT_MCH_ID = "1609876543";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrepayResponseDto createPrepay(PrepayRequestDto dto, Long userId) {
        // 1. 查询订单
        BookingOrder order = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderNo, dto.getOrderNo()));
        if (order == null) {
            throw new BusinessException("预下单失败：订单不存在 " + dto.getOrderNo());
        }

        if (order.getOrderStatus() != 0) {
            throw new BusinessException("预下单失败：订单非待支付锁定状态");
        }

        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已过 15 分钟支付有效期，已被释放");
        }

        String channel = dto.getChannel().toUpperCase();
        if (!Arrays.asList("ALIPAY", "WECHAT", "BALANCE").contains(channel)) {
            throw new BusinessException("不支持的支付渠道: " + channel);
        }

        LocalDateTime now = LocalDateTime.now();
        String timePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randSuffix = String.format("%04d", new Random().nextInt(10000));
        String tradeNo = channel.substring(0, 3) + timePrefix + randSuffix;

        // 如果是账户余额支付，直接执行扣款并出票
        if ("BALANCE".equals(channel)) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
                throw new BusinessException("账户余额不足 (当前余额: ￥" + user.getBalance() + "，需支付: ￥" + order.getTotalAmount() + ")");
            }

            user.setBalance(user.getBalance().subtract(order.getTotalAmount()));
            sysUserMapper.updateById(user);

            // 记录流水
            PaymentRecord record = PaymentRecord.builder()
                    .tradeNo(tradeNo)
                    .orderNo(order.getOrderNo())
                    .userId(userId)
                    .channel("BALANCE")
                    .amount(order.getTotalAmount())
                    .payStatus(1) // 支付成功
                    .gatewayTradeNo("BAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .buyerId(user.getUsername())
                    .signType("INTERNAL")
                    .notifyRawData("{\"source\":\"INTERNAL_BALANCE\",\"operator\":\"" + user.getUsername() + "\",\"amount\":\"" + order.getTotalAmount() + "\"}")
                    .reconciled(1)
                    .createTime(now)
                    .notifyTime(now)
                    .build();
            paymentRecordMapper.insert(record);

            // 修改订单为已支付，出票
            completeOrderPayment(order, channel);

            return PrepayResponseDto.builder()
                    .orderNo(order.getOrderNo())
                    .tradeNo(tradeNo)
                    .channel(channel)
                    .amount(order.getTotalAmount())
                    .payStatus(1)
                    .expireSeconds(0)
                    .createTime(now)
                    .build();
        }

        // 针对 支付宝 / 微信 渠道，创建待支付流水与专属扫码串
        PaymentRecord record = PaymentRecord.builder()
                .tradeNo(tradeNo)
                .orderNo(order.getOrderNo())
                .userId(userId)
                .channel(channel)
                .amount(order.getTotalAmount())
                .payStatus(0) // 待支付
                .signType("ALIPAY".equals(channel) ? "RSA2" : "HMAC-SHA256")
                .reconciled(1)
                .createTime(now)
                .build();
        paymentRecordMapper.insert(record);

        String qrCodeContent;
        if ("ALIPAY".equals(channel)) {
            qrCodeContent = "alipays://platformapi/startapp?appId=" + ALIPAY_SANDBOX_APPID + "&tradeNo=" + tradeNo + "&amount=" + order.getTotalAmount();
        } else {
            qrCodeContent = "weixin://wxpay/bizpayurl?pr=" + tradeNo + "&mchId=" + WECHAT_MCH_ID;
        }

        return PrepayResponseDto.builder()
                .orderNo(order.getOrderNo())
                .tradeNo(tradeNo)
                .channel(channel)
                .amount(order.getTotalAmount())
                .qrCodeContent(qrCodeContent)
                .expireSeconds(900)
                .sandboxNotifyMockUrl("/api/pay/mock-sandbox-callback")
                .payStatus(0)
                .createTime(now)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        log.info("【支付宝沙箱支付网关】收到异步 Webhook 回调通知: {}", params);

        String orderNo = params.get("out_trade_no");
        String gatewayTradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmountStr = params.get("total_amount");
        String buyerId = params.getOrDefault("buyer_id", "2088102179832104");

        // 1. 模拟 RSA2 验签逻辑 (验证商户 APP_ID 与合法性)
        String appId = params.get("app_id");
        if (appId != null && !appId.equals(ALIPAY_SANDBOX_APPID)) {
            log.warn("【支付宝沙箱】验签失败: APP_ID 不匹配 (expected: {}, actual: {})", ALIPAY_SANDBOX_APPID, appId);
            return "failure";
        }

        if (!"TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) && !"TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
            log.warn("【支付宝沙箱】非扣款成功状态: {}", tradeStatus);
            return "success";
        }

        // 2. 幂等性处理与入账
        processPaymentSuccess(orderNo, "ALIPAY", gatewayTradeNo, buyerId, "RSA2", params.toString());
        return "success";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handleWechatNotify(String notifyPayload) {
        log.info("【微信支付沙箱网关】收到异步 Webhook 回调通知: {}", notifyPayload);

        Map<String, String> response = new HashMap<>();
        try {
            // 解析 JSON Payload
            Map<String, Object> payloadMap = objectMapper.readValue(notifyPayload, Map.class);
            String orderNo = (String) payloadMap.get("out_trade_no");
            String gatewayTradeNo = (String) payloadMap.getOrDefault("transaction_id", "420000" + System.currentTimeMillis());
            String tradeState = (String) payloadMap.getOrDefault("trade_state", "SUCCESS");
            String buyerId = (String) payloadMap.getOrDefault("openid", "wx_openid_" + System.currentTimeMillis() % 10000);

            if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
                log.warn("【微信支付沙箱】非成功状态: {}", tradeState);
                response.put("code", "FAIL");
                response.put("message", "支付未成功");
                return response;
            }

            processPaymentSuccess(orderNo, "WECHAT", gatewayTradeNo, buyerId, "HMAC-SHA256", notifyPayload);

            response.put("code", "SUCCESS");
            response.put("message", "OK");
            return response;
        } catch (Exception e) {
            log.error("【微信支付沙箱】处理回调异常: ", e);
            response.put("code", "FAIL");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockSandboxCallback(String orderNo, String channel) {
        log.info("【沙箱演示触发器】用户一键模拟手机扫码支付成功: orderNo={}, channel={}", orderNo, channel);

        BookingOrder order = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNo);
        }

        String actualChannel = channel == null ? "ALIPAY" : channel.toUpperCase();
        String gatewayTradeNo;
        String buyerId;
        String signType;
        String mockPayload;

        if ("WECHAT".equals(actualChannel)) {
            gatewayTradeNo = "420000" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(1000));
            buyerId = "wx_sandbox_openid_" + String.format("%04d", new Random().nextInt(1000));
            signType = "HMAC-SHA256";

            Map<String, Object> wxNotify = new HashMap<>();
            wxNotify.put("mchid", WECHAT_MCH_ID);
            wxNotify.put("out_trade_no", orderNo);
            wxNotify.put("transaction_id", gatewayTradeNo);
            wxNotify.put("trade_state", "SUCCESS");
            wxNotify.put("trade_type", "NATIVE");
            wxNotify.put("bank_type", "CMC");
            wxNotify.put("openid", buyerId);
            wxNotify.put("amount", Map.of("total", order.getTotalAmount().multiply(new BigDecimal(100)).intValue(), "currency", "CNY"));
            wxNotify.put("success_time", LocalDateTime.now().toString());

            try {
                mockPayload = objectMapper.writeValueAsString(wxNotify);
            } catch (Exception e) {
                mockPayload = wxNotify.toString();
            }

            handleWechatNotify(mockPayload);
        } else {
            // 默认 ALIPAY
            gatewayTradeNo = "20260906220014" + String.format("%08d", new Random().nextInt(100000000));
            buyerId = "2088" + String.format("%012d", new Random().nextLong(1000000000000L));
            signType = "RSA2";

            Map<String, String> aliNotify = new HashMap<>();
            aliNotify.put("app_id", ALIPAY_SANDBOX_APPID);
            aliNotify.put("out_trade_no", orderNo);
            aliNotify.put("trade_no", gatewayTradeNo);
            aliNotify.put("trade_status", "TRADE_SUCCESS");
            aliNotify.put("total_amount", order.getTotalAmount().toString());
            aliNotify.put("buyer_id", buyerId);
            aliNotify.put("sign_type", "RSA2");
            aliNotify.put("sign", "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC63y9eYv7jR...");
            aliNotify.put("notify_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            mockPayload = aliNotify.toString();
            handleAlipayNotify(aliNotify);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderNo", orderNo);
        result.put("channel", actualChannel);
        result.put("gatewayTradeNo", gatewayTradeNo);
        result.put("buyerId", buyerId);
        result.put("signType", signType);
        result.put("amount", order.getTotalAmount());
        result.put("message", "沙箱网关异步回调处理完成，订单已成功出票！");
        return result;
    }

    /**
     * 核心统一支付完成逻辑 (幂等入账、出票、实时推送)
     */
    private synchronized void processPaymentSuccess(String orderNo, String channel, String gatewayTradeNo,
                                                   String buyerId, String signType, String rawPayload) {
        BookingOrder order = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderNo, orderNo));
        if (order == null) {
            log.warn("【支付网关】未找到对应订单: {}", orderNo);
            return;
        }

        // 幂等校验：已支付直接退出
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            log.info("【支付网关】订单 {} 已处于已支付状态，忽略重复通知", orderNo);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新流水记录
        PaymentRecord record = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, orderNo)
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 1"));

        if (record != null) {
            record.setPayStatus(1);
            record.setGatewayTradeNo(gatewayTradeNo);
            record.setBuyerId(buyerId);
            record.setSignType(signType);
            record.setNotifyRawData(rawPayload);
            record.setNotifyTime(now);
            record.setReconciled(1);
            paymentRecordMapper.updateById(record);
        } else {
            record = PaymentRecord.builder()
                    .tradeNo(channel.substring(0, 3) + System.currentTimeMillis())
                    .orderNo(orderNo)
                    .userId(order.getUserId())
                    .channel(channel)
                    .amount(order.getTotalAmount())
                    .payStatus(1)
                    .gatewayTradeNo(gatewayTradeNo)
                    .buyerId(buyerId)
                    .signType(signType)
                    .notifyRawData(rawPayload)
                    .reconciled(1)
                    .createTime(now)
                    .notifyTime(now)
                    .build();
            paymentRecordMapper.insert(record);
        }

        // 订单状态变更为已支付，出票
        completeOrderPayment(order, channel);
    }

    private void completeOrderPayment(BookingOrder order, String channel) {
        LocalDateTime now = LocalDateTime.now();
        String verifyCode = order.getVerifyCode();
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            verifyCode = String.format("%06d", new Random().nextInt(900000) + 100000);
        }

        order.setPayStatus(1); // 已支付
        order.setOrderStatus(1); // 预约成功(待核销)
        order.setPayTime(now);
        order.setVerifyCode(verifyCode);
        order.setUpdateTime(now);
        bookingOrderMapper.updateById(order);

        Venue venue = venueMapper.selectById(order.getVenueId());
        String venueName = venue != null ? venue.getName() : "场地";

        // WebSocket 全局广播
        webSocketPushService.broadcastSlotChange(SlotEventDto.builder()
                .eventType("PAY")
                .venueId(order.getVenueId())
                .venueName(venueName)
                .bookDate(order.getBookDate())
                .timeSlot(order.getTimeSlot())
                .status(2) // 已预约
                .userId(order.getUserId())
                .message("场地【" + venueName + "】时段 " + order.getTimeSlot() + " 已通过 " + channel + " 支付出票")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("【支付网关】订单支付成功入账并出票: orderNo={}, channel={}, verifyCode={}",
                order.getOrderNo(), channel, verifyCode);
    }

    @Override
    public ReconciliationSummaryVo getReconciliationSummary() {
        // 查询最近流水记录
        List<PaymentRecord> paymentRecords = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 200"));

        // 查询对应订单
        Set<String> orderNos = paymentRecords.stream().map(PaymentRecord::getOrderNo).collect(Collectors.toSet());
        Map<String, BookingOrder> orderMap = new HashMap<>();
        if (!orderNos.isEmpty()) {
            List<BookingOrder> orders = bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                    .in(BookingOrder::getOrderNo, orderNos));
            for (BookingOrder o : orders) {
                orderMap.put(o.getOrderNo(), o);
            }
        }

        // 场地映射
        Map<Long, Venue> venueMap = venueMapper.selectList(null).stream()
                .collect(Collectors.toMap(Venue::getId, v -> v, (k1, k2) -> k1));

        List<PaymentReconciliationVo> vos = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal alipayIncome = BigDecimal.ZERO;
        BigDecimal wechatIncome = BigDecimal.ZERO;
        BigDecimal balanceIncome = BigDecimal.ZERO;
        int matched = 0;
        int exception = 0;

        for (PaymentRecord r : paymentRecords) {
            BookingOrder o = orderMap.get(r.getOrderNo());
            Venue v = (o != null && o.getVenueId() != null) ? venueMap.get(o.getVenueId()) : null;

            String reconcileStatus = "MATCH";
            String reconcileMessage = "流水与系统账单核对一致 (平账)";

            if (o == null) {
                reconcileStatus = "ORDER_MISSING";
                reconcileMessage = "未匹配到系统预约订单";
                exception++;
            } else if (r.getPayStatus() == 1 && o.getPayStatus() != 1) {
                reconcileStatus = "STATUS_MISMATCH";
                reconcileMessage = "网关显示已扣款，但系统订单未标记为已支付";
                exception++;
            } else if (r.getAmount().compareTo(o.getTotalAmount()) != 0) {
                reconcileStatus = "AMOUNT_MISMATCH";
                reconcileMessage = "网关实收金额 (" + r.getAmount() + ") 与订单应收金额 (" + o.getTotalAmount() + ") 不一致";
                exception++;
            } else {
                matched++;
            }

            if (r.getPayStatus() == 1) {
                totalIncome = totalIncome.add(r.getAmount());
                if ("ALIPAY".equalsIgnoreCase(r.getChannel())) {
                    alipayIncome = alipayIncome.add(r.getAmount());
                } else if ("WECHAT".equalsIgnoreCase(r.getChannel())) {
                    wechatIncome = wechatIncome.add(r.getAmount());
                } else if ("BALANCE".equalsIgnoreCase(r.getChannel())) {
                    balanceIncome = balanceIncome.add(r.getAmount());
                }
            }

            vos.add(PaymentReconciliationVo.builder()
                    .tradeNo(r.getTradeNo())
                    .orderNo(r.getOrderNo())
                    .venueName(v != null ? v.getName() : "—")
                    .contactName(o != null ? o.getContactName() : "—")
                    .channel(r.getChannel())
                    .orderAmount(o != null ? o.getTotalAmount() : r.getAmount())
                    .gatewayAmount(r.getAmount())
                    .orderStatus(o != null ? o.getOrderStatus() : -1)
                    .payStatus(r.getPayStatus())
                    .gatewayTradeNo(r.getGatewayTradeNo())
                    .buyerId(r.getBuyerId())
                    .reconcileStatus(reconcileStatus)
                    .reconcileMessage(reconcileMessage)
                    .createTime(r.getCreateTime())
                    .notifyTime(r.getNotifyTime())
                    .build());
        }

        int totalTx = paymentRecords.size();
        double matchRate = totalTx == 0 ? 100.0 : Math.round((double) matched / totalTx * 1000.0) / 10.0;

        return ReconciliationSummaryVo.builder()
                .totalIncome(totalIncome)
                .alipayIncome(alipayIncome)
                .wechatIncome(wechatIncome)
                .balanceIncome(balanceIncome)
                .totalTransactions(totalTx)
                .matchedCount(matched)
                .exceptionCount(exception)
                .matchRate(matchRate)
                .records(vos)
                .build();
    }
}
