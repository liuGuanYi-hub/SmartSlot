package com.smartslot.controller;

import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.LoginDto;
import com.smartslot.dto.RegisterDto;
import com.smartslot.entity.SysUser;
import com.smartslot.service.SysUserService;
import com.smartslot.vo.LoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户与鉴权认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVo> login(@Valid @RequestBody LoginDto dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDto dto) {
        userService.register(dto);
        return Result.success("注册成功，请使用新账号登录", null);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<SysUser> getCurrentUser() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getCurrentUserInfo(userId));
    }
}
