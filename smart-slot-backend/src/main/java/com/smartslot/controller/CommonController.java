package com.smartslot.controller;

import com.smartslot.common.Result;
import com.smartslot.service.IdempotentTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用基础设施接口
 */
@Tag(name = "通用系统接口")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final IdempotentTokenService idempotentTokenService;

    @Operation(summary = "获取一次性接口防重放幂等 Token")
    @GetMapping("/idempotent-token")
    public Result<String> getIdempotentToken() {
        String token = idempotentTokenService.generateToken();
        return Result.success("获取幂等凭证成功", token);
    }
}
