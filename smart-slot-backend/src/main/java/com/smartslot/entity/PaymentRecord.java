package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付网关流水与对账实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 系统内交易流水号 (以 ALI/WX/BAL 开头)
     */
    private String tradeNo;

    /**
     * 关联预约订单号 (ORD...)
     */
    private String orderNo;

    /**
     * 付款用户 ID
     */
    private Long userId;

    /**
     * 支付渠道: ALIPAY(支付宝), WECHAT(微信支付), BALANCE(账户余额)
     */
    private String channel;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付状态: 0-待支付, 1-支付成功, 2-支付失败, 3-已退款
     */
    private Integer payStatus;

    /**
     * 第三方网关交易凭证号 (如支付宝 trade_no / 微信 transaction_id)
     */
    private String gatewayTradeNo;

    /**
     * 买家第三方账号标识
     */
    private String buyerId;

    /**
     * 签名校验方式 (RSA2 / HMAC-SHA256)
     */
    private String signType;

    /**
     * 网关异步通知原始 Payload 报文 (JSON / XML)
     */
    private String notifyRawData;

    /**
     * 是否已平账: 1-平账匹配, 0-账单异常
     */
    private Integer reconciled;

    /**
     * 预下单发起时间
     */
    private LocalDateTime createTime;

    /**
     * 网关异步回调入账时间
     */
    private LocalDateTime notifyTime;
}
