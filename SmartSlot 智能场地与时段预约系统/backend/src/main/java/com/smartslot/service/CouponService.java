package com.smartslot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.CouponOptimalDto;
import com.smartslot.entity.Coupon;
import com.smartslot.entity.UserCoupon;
import com.smartslot.vo.CouponOptimalVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营销优惠券核心服务接口
 */
public interface CouponService extends IService<Coupon> {

    /**
     * 领券中心：获取全部上架优惠券，附带当前用户是否已领取标记
     */
    List<Coupon> listClaimableCoupons(Long userId);

    /**
     * 用户领取优惠券
     */
    UserCoupon claimCoupon(Long couponId, Long userId);

    /**
     * 查询我的卡券包
     * @param status 0-未使用, 1-已使用, 2-已过期, null-全部
     */
    List<UserCoupon> listMyCoupons(Long userId, Integer status);

    /**
     * 智能优选算价引擎 (自动匹配当前订单最优卡券)
     */
    CouponOptimalVo calculateOptimal(CouponOptimalDto dto, Long userId);

    /**
     * 订单结算核销优惠券
     */
    BigDecimal consumeUserCoupon(Long userCouponId, Long userId, BigDecimal orderAmount, String orderNo);

    /**
     * 订单取消时回滚退还优惠券
     */
    void rollbackUserCoupon(String orderNo);
}
