package com.smartslot.controller;

import com.smartslot.annotation.LogRecord;
import com.smartslot.annotation.RequiresRoles;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.constant.UserRole;
import com.smartslot.entity.IotGateLog;
import com.smartslot.service.IotGateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "物联网智能门禁与道闸接口")
@RestController
@RequestMapping("/api/iot/gate")
@RequiredArgsConstructor
@RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER, UserRole.ROLE_VERIFIER})
public class IotGateController {

    private final IotGateService iotGateService;

    @Operation(summary = "手动远程下发应急开闸指令")
    @LogRecord(module = "门禁道闸中控", operation = "手动远程开闸")
    @PostMapping("/manual-open")
    public Result<IotGateLog> manualOpenGate(
            @RequestParam String gateId,
            @RequestParam(defaultValue = "EMERGENCY_OPEN") String action
    ) {
        String operator = UserContext.getUsername();
        if (operator == null) operator = "admin";
        IotGateLog log = iotGateService.manualControlGate(gateId, action, operator);
        return Result.success("指令已成功通过 MQTT / Webhook 下发至道闸终端", log);
    }

    @Operation(summary = "获取实时道闸通讯报文日志流")
    @GetMapping("/logs")
    public Result<List<IotGateLog>> getGateLogs(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(iotGateService.getRecentLogs(limit));
    }

    @Operation(summary = "获取场馆所有智能门禁与道闸硬件状态")
    @GetMapping("/devices")
    public Result<List<Map<String, Object>>> getGateDevices() {
        return Result.success(iotGateService.getGateDevices());
    }
}
