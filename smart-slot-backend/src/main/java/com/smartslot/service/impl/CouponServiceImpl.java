package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.CouponOptimalDto;
import com.smartslot.entity.Coupon;
import com.smartslot.entity.UserCoupon;
import com.smartslot.mapper.CouponMapper;
import com.smartslot.mapper.UserCouponMapper;
import com.smartslot.service.CouponService;
import com.smartslot.vo.CouponOptimalVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private final UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> listClaimableCoupons(Long userId) {
        List<Coupon> coupons = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .orderByAsc(Coupon::getId));

        if (userId != null && !coupons.isEmpty()) {
            List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId));
            Set<Long> claimedCouponIds = userCoupons.stream()
                    .map(UserCoupon::getCouponId)
                    .collect(Collectors.toSet());

            for (Coupon c : coupons) {
                // 如果当前用户已领取过，则通过 description 或其他方式感知，也可在前端标记
                if (claimedCouponIds.contains(c.getId())) {
                    // 标记为已拥有
                    c.setDescription((c.getDescription() == null ? "" : c.getDescription()) + " [已领取]");
                }
            }
        }
        return coupons;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon claimCoupon(Long couponId, Long userId) {
        Coupon coupon = getById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("该优惠券已下架或不存在");
        }

        if (coupon.getTotalCount() > 0 && coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new BusinessException("手慢了，该优惠券已被抢光！");
        }

        // 检查该用户是否已领取过且尚未使用
        Long existingCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0));
        if (existingCount > 0) {
            throw new BusinessException("您已领取过该优惠券，请在我的卡券包中查看使用");
        }

        // 累加已领数量
        coupon.setClaimedCount(coupon.getClaimedCount() + 1);
        updateById(coupon);

        LocalDateTime now = LocalDateTime.now();
        int days = coupon.getValidDays() != null && coupon.getValidDays() > 0 ? coupon.getValidDays() : 30;

        UserCoupon userCoupon = UserCoupon.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .status(0) // 0-未使用
                .claimTime(now)
                .expireTime(now.plusDays(days))
                .build();

        userCouponMapper.insert(userCoupon);

        enrichUserCouponMeta(userCoupon, coupon);
        log.info("用户领取优惠券成功: userId={}, couponId={}, userCouponId={}", userId, couponId, userCoupon.getId());
        return userCoupon;
    }

    @Override
    public List<UserCoupon> listMyCoupons(Long userId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByAsc(UserCoupon::getStatus)
                .orderByAsc(UserCoupon::getExpireTime));

        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询 coupon 模板信息
        Set<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = listByIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        List<UserCoupon> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            // 自动检查过期状态
            if (uc.getStatus() == 0 && uc.getExpireTime() != null && uc.getExpireTime().isBefore(now)) {
                uc.setStatus(2); // 已过期
                userCouponMapper.updateById(uc);
            }
            if (status == null || status.equals(uc.getStatus())) {
                Coupon c = couponMap.get(uc.getCouponId());
                if (c != null) {
                    enrichUserCouponMeta(uc, c);
                }
                result.add(uc);
            }
        }
        return result;
    }

    @Override
    public CouponOptimalVo calculateOptimal(CouponOptimalDto dto, Long userId) {
        BigDecimal orderAmount = dto.getAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponOptimalVo.builder()
                    .originalAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(BigDecimal.ZERO)
                    .availableCoupons(Collections.emptyList())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> myCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .gt(UserCoupon::getExpireTime, now));

        if (myCoupons.isEmpty()) {
            return CouponOptimalVo.builder()
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .availableCoupons(Collections.emptyList())
                    .build();
        }

        Set<Long> couponIds = myCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = listByIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        List<UserCoupon> availableCoupons = new ArrayList<>();
        Map<Long, BigDecimal> discountMap = new HashMap<>();

        for (UserCoupon uc : myCoupons) {
            Coupon c = couponMap.get(uc.getCouponId());
            if (c == null) continue;

            // 品类限定检查
            if (c.getCategoryId() != null && dto.getVenueId() != null) {
                // 如果限定品类，可进一步比对，这里暂允许通用
            }

            // 门槛检查
            BigDecimal minSpend = c.getMinSpend() != null ? c.getMinSpend() : BigDecimal.ZERO;
            if (orderAmount.compareTo(minSpend) >= 0) {
                enrichUserCouponMeta(uc, c);
                BigDecimal discount = calculateDiscount(c, orderAmount);
                discountMap.put(uc.getId(), discount);
                availableCoupons.add(uc);
            }
        }

        if (availableCoupons.isEmpty()) {
            return CouponOptimalVo.builder()
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .availableCoupons(Collections.emptyList())
                    .build();
        }

        // 按优惠金额从大到小排序
        availableCoupons.sort((a, b) -> discountMap.get(b.getId()).compareTo(discountMap.get(a.getId())));

        UserCoupon best = availableCoupons.get(0);
        BigDecimal maxDiscount = discountMap.get(best.getId());
        BigDecimal finalAmount = orderAmount.subtract(maxDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalAmount = new BigDecimal("0.01"); // 最低付1分钱保真
            maxDiscount = orderAmount.subtract(finalAmount);
        }

        return CouponOptimalVo.builder()
                .originalAmount(orderAmount)
                .discountAmount(maxDiscount)
                .finalAmount(finalAmount)
                .bestCoupon(best)
                .availableCoupons(availableCoupons)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal consumeUserCoupon(Long userCouponId, Long userId, BigDecimal orderAmount, String orderNo) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }

        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("该优惠券不存在或不属于当前用户");
        }

        if (uc.getStatus() != 0) {
            throw new BusinessException("该优惠券已被使用或已失效");
        }

        if (uc.getExpireTime() != null && uc.getExpireTime().isBefore(LocalDateTime.now())) {
            uc.setStatus(2);
            userCouponMapper.updateById(uc);
            throw new BusinessException("该优惠券已过期");
        }

        Coupon c = getById(uc.getCouponId());
        if (c == null) {
            throw new BusinessException("优惠券模板失效");
        }

        if (orderAmount.compareTo(c.getMinSpend()) < 0) {
            throw new BusinessException("未达到优惠券满减门槛 (满 ￥" + c.getMinSpend() + " 可用)");
        }

        BigDecimal discount = calculateDiscount(c, orderAmount);
        if (discount.compareTo(orderAmount) >= 0) {
            discount = orderAmount.subtract(new BigDecimal("0.01"));
        }

        uc.setStatus(1); // 已使用
        uc.setUsedTime(LocalDateTime.now());
        uc.setOrderNo(orderNo);
        userCouponMapper.updateById(uc);

        log.info("优惠券核销成功: userCouponId={}, orderNo={}, discount={}", userCouponId, orderNo, discount);
        return discount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUserCoupon(String orderNo) {
        if (orderNo == null) return;
        List<UserCoupon> list = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getOrderNo, orderNo));
        for (UserCoupon uc : list) {
            uc.setStatus(0); // 恢复未使用
            uc.setUsedTime(null);
            uc.setOrderNo(null);
            userCouponMapper.updateById(uc);
            log.info("退订自动归还优惠券: userCouponId={}, orderNo={}", uc.getId(), orderNo);
        }
    }

    private BigDecimal calculateDiscount(Coupon c, BigDecimal amount) {
        if (c.getType() == 1 || c.getType() == 3) {
            // 满减或立减
            return c.getDiscountAmount() != null ? c.getDiscountAmount() : BigDecimal.ZERO;
        } else if (c.getType() == 2) {
            // 折扣券 (如 0.85 折扣，优惠额为 1 - 0.85 = 0.15)
            BigDecimal rate = c.getDiscountRate() != null ? c.getDiscountRate() : BigDecimal.ONE;
            BigDecimal discountPart = BigDecimal.ONE.subtract(rate);
            return amount.multiply(discountPart).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private void enrichUserCouponMeta(UserCoupon uc, Coupon c) {
        uc.setCouponName(c.getName());
        uc.setCouponType(c.getType());
        uc.setMinSpend(c.getMinSpend());
        uc.setDiscountAmount(c.getDiscountAmount());
        uc.setDiscountRate(c.getDiscountRate());
        uc.setDescription(c.getDescription());
    }
}
