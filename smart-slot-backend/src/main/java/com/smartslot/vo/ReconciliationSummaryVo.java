package com.smartslot.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务对账汇总视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummaryVo {

    private BigDecimal totalIncome;

    private BigDecimal alipayIncome;

    private BigDecimal wechatIncome;

    private BigDecimal balanceIncome;

    private Integer totalTransactions;

    private Integer matchedCount;

    private Integer exceptionCount;

    private Double matchRate;

    private List<PaymentReconciliationVo> records;
}
