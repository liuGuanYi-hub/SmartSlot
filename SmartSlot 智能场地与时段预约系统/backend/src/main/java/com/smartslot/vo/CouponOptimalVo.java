package com.smartslot.vo;

import com.smartslot.entity.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券智能算价 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponOptimalVo {

    private BigDecimal originalAmount; // 原价

    private BigDecimal discountAmount; // 抵扣金额

    private BigDecimal finalAmount; // 券后实付金额

    private UserCoupon bestCoupon; // 推荐的最优优惠券

    private List<UserCoupon> availableCoupons; // 当前所有可用优惠券
}
