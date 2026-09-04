package com.smartslot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.common.PageResult;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.BookingCreateDto;
import com.smartslot.dto.ReviewCreateDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.service.BookingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预约与订单管理接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingOrderController {

    private final BookingOrderService orderService;

    @Operation(summary = "锁定并创建预约订单 (核心防超卖接口)")
    @PostMapping("/orders/lock-and-create")
    public Result<BookingOrder> lockAndCreateOrder(@Valid @RequestBody BookingCreateDto dto) {
        Long userId = UserContext.getUserId();
        return Result.success("时段锁定成功，请在 15 分钟内完成支付", orderService.lockAndCreateOrder(dto, userId));
    }

    @Operation(summary = "模拟支付订单 (生成 6 位核销码)")
    @PostMapping("/orders/pay/{orderNo}")
    public Result<BookingOrder> payOrder(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        return Result.success("支付成功，已为您生成专属核销凭证", orderService.payOrder(orderNo, userId));
    }

    @Operation(summary = "取消预约订单")
    @PostMapping("/orders/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Long userId = UserContext.getUserId();
        orderService.cancelOrder(id, userId, reason);
        return Result.success("订单取消成功，相关费用已原路返还", null);
    }

    @Operation(summary = "用户端: 分页查询我的预约历史 (MyBatis-Plus 分页插件)")
    @GetMapping("/orders/my")
    public Result<PageResult<BookingOrder>> pageMyOrders(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        Page<BookingOrder> page = orderService.pageUserOrders(new Page<>(current, size), userId, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages()));
    }

    @Operation(summary = "发表评价与打分")
    @PostMapping("/orders/review")
    public Result<Void> addReview(@Valid @RequestBody ReviewCreateDto dto) {
        Long userId = UserContext.getUserId();
        orderService.addReview(dto, userId);
        return Result.success("评价发布成功，感谢您的反馈！", null);
    }

    // ==========================================
    // 管理后台订单管理与核销 (MyBatis-Plus 分页插件)
    // ==========================================

    @Operation(summary = "管理端: 专属 6 位核销码快速核验")
    @PostMapping("/admin/orders/verify")
    public Result<BookingOrder> verifyOrder(@RequestParam String verifyCode) {
        return Result.success("核销成功，已允许入场消费", orderService.verifyOrder(verifyCode));
    }

    @Operation(summary = "管理端: 订单列表多条件组合分页检索 (MyBatis-Plus 分页插件)")
    @GetMapping("/admin/orders/page")
    public Result<PageResult<BookingOrder>> pageAdminOrders(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        Page<BookingOrder> page = orderService.pageAdminOrders(new Page<>(current, size), orderNo, phone, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages()));
    }
}
