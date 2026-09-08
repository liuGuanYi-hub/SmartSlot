package com.smartslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券最优抵扣试算 DTO
 */
@Data
public class CouponOptimalDto {

    private Long venueId;

    @NotNull(message = "订单原价不能为空")
    private BigDecimal amount;
}
