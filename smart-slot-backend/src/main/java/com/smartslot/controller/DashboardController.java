package com.smartslot.controller;

import com.smartslot.common.Result;
import com.smartslot.service.DashboardService;
import com.smartslot.vo.DashboardVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运营统计看板接口")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取运营统计核心指标与 ECharts 图表数据")
    @GetMapping("/stats")
    public Result<DashboardVo> getDashboardStats() {
        return Result.success(dashboardService.getDashboardStats());
    }
}
