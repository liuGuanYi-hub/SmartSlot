package com.smartslot.controller;

import com.smartslot.annotation.LogRecord;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.CouponOptimalDto;
import com.smartslot.entity.Coupon;
import com.smartslot.entity.UserCoupon;
import com.smartslot.service.CouponService;
import com.smartslot.vo.CouponOptimalVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "营销优惠券与满减引擎接口")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心：获取全部上架发放中优惠券")
    @GetMapping("/list")
    public Result<List<Coupon>> listClaimable() {
        Long userId = UserContext.getUserId();
        List<Coupon> list = couponService.listClaimableCoupons(userId);
        return Result.success(list);
    }

    @Operation(summary = "用户领取指定优惠券")
    @LogRecord(module = "营销优惠券", operation = "领取优惠券")
    @PostMapping("/claim/{id}")
    public Result<UserCoupon> claim(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        UserCoupon uc = couponService.claimCoupon(id, userId);
        return Result.success("优惠券领取成功", uc);
    }

    @Operation(summary = "查询当前用户卡券包列表")
    @GetMapping("/my")
    public Result<List<UserCoupon>> listMy(@RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        List<UserCoupon> list = couponService.listMyCoupons(userId, status);
        return Result.success(list);
    }

    @Operation(summary = "订单结算智能优选抵扣试算")
    @PostMapping("/optimal")
    public Result<CouponOptimalVo> calculateOptimal(@Valid @RequestBody CouponOptimalDto dto) {
        Long userId = UserContext.getUserId();
        CouponOptimalVo vo = couponService.calculateOptimal(dto, userId);
        return Result.success(vo);
    }
}
