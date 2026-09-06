package com.smartslot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.annotation.RequiresRoles;
import com.smartslot.common.PageResult;
import com.smartslot.common.Result;
import com.smartslot.constant.UserRole;
import com.smartslot.entity.OperationLog;
import com.smartslot.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-操作审计日志")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@RequiresRoles(UserRole.ROLE_ADMIN)
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询操作审计日志")
    @GetMapping
    public Result<PageResult<OperationLog>> pageLogs(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username
    ) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        wrapper.orderByDesc(OperationLog::getId);

        Page<OperationLog> logPage = operationLogService.page(page, wrapper);
        return Result.success(new PageResult<>(
                logPage.getRecords(),
                logPage.getTotal(),
                logPage.getCurrent(),
                logPage.getSize(),
                logPage.getPages()
        ));
    }
}
