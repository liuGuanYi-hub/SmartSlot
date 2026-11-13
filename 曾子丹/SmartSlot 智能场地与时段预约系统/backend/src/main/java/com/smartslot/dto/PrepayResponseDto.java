package com.smartslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepayResponseDto {

    private String orderNo;

    private String tradeNo;

    private String channel;

    private BigDecimal amount;

    /**
     * 客户端收银台二维码协议串 (例如支付宝支付串、微信支付链接或本地 Base64 二维码数据)
     */
    private String qrCodeContent;

    /**
     * 支付有效倒计时 (秒)
     */
    private Integer expireSeconds;

    /**
     * 沙箱模拟异步回调通知的测试端点
     */
    private String sandboxNotifyMockUrl;

    /**
     * 支付状态: 0-待扫码支付, 1-已直接完成扣款(如余额支付)
     */
    private Integer payStatus;

    private LocalDateTime createTime;
}
