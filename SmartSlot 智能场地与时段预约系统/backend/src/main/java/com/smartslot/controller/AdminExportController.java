package com.smartslot.controller;

import com.smartslot.annotation.LogRecord;
import com.smartslot.annotation.RequiresRoles;
import com.smartslot.constant.UserRole;
import com.smartslot.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "管理端报表导出中心")
@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
public class AdminExportController {

    private final ExportService exportService;

    @Operation(summary = "导出订单流水与预约对账单 (Excel)")
    @LogRecord(module = "报表中心", operation = "导出订单流水对账单")
    @GetMapping("/orders")
    public void exportOrders(
            HttpServletResponse response,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status
    ) throws IOException {
        exportService.exportBookingOrders(response, startDate, endDate, status);
    }

    @Operation(summary = "导出场馆配置与排期总表 (Excel)")
    @LogRecord(module = "报表中心", operation = "导出场地排期总表")
    @GetMapping("/venues")
    public void exportVenues(HttpServletResponse response) throws IOException {
        exportService.exportVenues(response);
    }
}
