package com.smartslot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.PrepayRequestDto;
import com.smartslot.dto.PrepayResponseDto;
import com.smartslot.entity.PaymentRecord;
import com.smartslot.vo.ReconciliationSummaryVo;

import java.util.Map;

public interface PaymentGatewayService extends IService<PaymentRecord> {

    /**
     * 发起多渠道收银台预下单 (生成第三方支付凭证与专属二维码)
     */
    PrepayResponseDto createPrepay(PrepayRequestDto dto, Long userId);

    /**
     * 支付宝沙箱/真实异步 Webhook 回调验签与入账处理
     */
    String handleAlipayNotify(Map<String, String> params);

    /**
     * 微信支付沙箱/真实异步 Webhook 回调验签与入账处理
     */
    Map<String, String> handleWechatNotify(String notifyPayload);

    /**
     * 一键触发沙箱模拟异步回调通知 (演示全链路扫码扣款与自动化出票)
     */
    Map<String, Object> mockSandboxCallback(String orderNo, String channel);

    /**
     * 获取全渠道财务对账审计汇总与明细对账单
     */
    ReconciliationSummaryVo getReconciliationSummary();
}
