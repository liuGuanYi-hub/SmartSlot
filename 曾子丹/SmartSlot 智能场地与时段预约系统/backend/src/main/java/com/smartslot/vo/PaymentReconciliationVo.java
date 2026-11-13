package com.smartslot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水与订单对账视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReconciliationVo {

    private String tradeNo;

    private String orderNo;

    private String venueName;

    private String contactName;

    private String channel;

    private BigDecimal orderAmount;

    private BigDecimal gatewayAmount;

    private Integer orderStatus; // 0-待支付锁定中, 1-预约成功(待核销), 2-已完成(已核销), 3-已取消

    private Integer payStatus; // 0-未支付, 1-已支付, 2-已退款

    private String gatewayTradeNo;

    private String buyerId;

    /**
     * 对账结论: MATCH(对账一致平账), AMOUNT_MISMATCH(金额不一致), STATUS_MISMATCH(状态不一致), ORDER_MISSING(未找到订单)
     */
    private String reconcileStatus;

    private String reconcileMessage;

    private LocalDateTime createTime;

    private LocalDateTime notifyTime;
}
