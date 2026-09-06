package com.smartslot.controller;

import com.smartslot.annotation.Idempotent;
import com.smartslot.annotation.LogRecord;
import com.smartslot.annotation.RequiresRoles;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.constant.UserRole;
import com.smartslot.dto.PrepayRequestDto;
import com.smartslot.dto.PrepayResponseDto;
import com.smartslot.service.PaymentGatewayService;
import com.smartslot.vo.ReconciliationSummaryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "多渠道支付网关与财务对账接口")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @Operation(summary = "多渠道收银台预下单 (生成支付宝/微信动态二维码)")
    @Idempotent(message = "正在发起支付预下单，请勿重复提交")
    @PostMapping("/prepay")
    public Result<PrepayResponseDto> prepay(@Valid @RequestBody PrepayRequestDto dto) {
        Long userId = UserContext.getUserId();
        PrepayResponseDto responseDto = paymentGatewayService.createPrepay(dto, userId);
        return Result.success("预下单创建成功", responseDto);
    }

    @Operation(summary = "支付宝沙箱/生产异步 Webhook 通知回调")
    @PostMapping(value = "/notify/alipay", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE})
    public String alipayNotify(@RequestParam Map<String, String> params) {
        return paymentGatewayService.handleAlipayNotify(params);
    }

    @Operation(summary = "微信支付沙箱/生产异步 Webhook 通知回调")
    @PostMapping(value = "/notify/wechat", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    public Map<String, String> wechatNotify(@RequestBody String payload) {
        return paymentGatewayService.handleWechatNotify(payload);
    }

    @Operation(summary = "一键模拟沙箱手机扫码扣款成功 (触发网关异步通知)")
    @LogRecord(module = "支付网关", operation = "模拟第三方支付扣款回调")
    @PostMapping("/mock-sandbox-callback")
    public Result<Map<String, Object>> mockSandboxCallback(@RequestBody Map<String, String> requestBody) {
        String orderNo = requestBody.get("orderNo");
        String channel = requestBody.getOrDefault("channel", "ALIPAY");
        Map<String, Object> result = paymentGatewayService.mockSandboxCallback(orderNo, channel);
        return Result.success("沙箱扣款成功，已触发网关异步通知出票", result);
    }

    @Operation(summary = "查询财务网关流水对账单")
    @RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
    @GetMapping("/reconciliation")
    public Result<ReconciliationSummaryVo> getReconciliation() {
        return Result.success(paymentGatewayService.getReconciliationSummary());
    }
}
