package com.smartslot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.annotation.LogRecord;
import com.smartslot.annotation.RequiresRoles;
import com.smartslot.common.BusinessException;
import com.smartslot.common.PageResult;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.constant.UserRole;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.PaymentRecord;
import com.smartslot.entity.SysUser;
import com.smartslot.mapper.BookingOrderMapper;
import com.smartslot.mapper.PaymentRecordMapper;
import com.smartslot.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "用户个人中心与会员中台管理接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final PaymentRecordMapper paymentRecordMapper;
    private final BookingOrderMapper bookingOrderMapper;

    // ==========================================
    // 1. 用户个人中心 (Profile & Wallet)
    // ==========================================

    @Operation(summary = "获取当前登录用户档案与履约统计")
    @GetMapping("/user/profile")
    public Result<UserProfileVo> getCurrentUserProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录或凭证已过期");
        }
        SysUser user = sysUserService.getCurrentUserInfo(userId);

        // 统计履约历史数据
        Long totalBookings = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId));
        Long completedBookings = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId)
                .eq(BookingOrder::getOrderStatus, 2)); // 已核销完成
        Long canceledBookings = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId)
                .eq(BookingOrder::getOrderStatus, 3)); // 已取消
        Long activeBookings = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId)
                .in(BookingOrder::getOrderStatus, List.of(0, 1))); // 待支付或已预约待核销

        UserProfileVo vo = UserProfileVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .balance(user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO)
                .creditScore(user.getCreditScore() != null ? user.getCreditScore() : 100)
                .createTime(user.getCreateTime())
                .totalBookings(totalBookings != null ? totalBookings : 0L)
                .completedBookings(completedBookings != null ? completedBookings : 0L)
                .canceledBookings(canceledBookings != null ? canceledBookings : 0L)
                .activeBookings(activeBookings != null ? activeBookings : 0L)
                .build();

        return Result.success(vo);
    }

    @Operation(summary = "修改个人资料(昵称、手机号)")
    @LogRecord(module = "个人中心", operation = "修改个人基础资料")
    @PutMapping("/user/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String nickname = body.get("nickname");
        String phone = body.get("phone");
        sysUserService.updateProfile(userId, nickname, phone);
        return Result.success("个人资料修改成功", null);
    }

    @Operation(summary = "修改登录密码")
    @LogRecord(module = "个人中心", operation = "修改登录密码")
    @PutMapping("/user/password")
    public Result<Void> updatePassword(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        sysUserService.updatePassword(userId, oldPassword, newPassword);
        return Result.success("登录密码修改成功，请牢记新密码", null);
    }

    @Operation(summary = "会员钱包充值(赠金自动结算)")
    @LogRecord(module = "会员钱包", operation = "在线充值入账")
    @PostMapping("/user/wallet/recharge")
    public Result<SysUser> rechargeWallet(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        String channel = (String) body.getOrDefault("channel", "ALIPAY");
        SysUser user = sysUserService.rechargeWallet(userId, amount, channel);
        return Result.success("充值成功，赠金已自动入账", user);
    }

    @Operation(summary = "获取当前用户资金变动流水台账")
    @GetMapping("/user/wallet/records")
    public Result<List<PaymentRecord>> getMyWalletRecords() {
        Long userId = UserContext.getUserId();
        List<PaymentRecord> records = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getUserId, userId)
                .orderByDesc(PaymentRecord::getId));
        return Result.success(records);
    }

    // ==========================================
    // 2. 管理后台会员中台 (Admin User Operations)
    // ==========================================

    @Operation(summary = "管理端: 会员用户列表分页查询")
    @RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
    @GetMapping("/admin/users/page")
    public Result<PageResult<SysUser>> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword.trim())
                    .or().like(SysUser::getNickname, keyword.trim())
                    .or().like(SysUser::getPhone, keyword.trim()));
        }
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(SysUser::getRole, role.trim());
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserService.page(new Page<>(current, size), wrapper);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages()));
    }

    @Operation(summary = "管理端: 切换账号状态(启用/禁用)")
    @RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
    @LogRecord(module = "会员管理", operation = "修改用户账号启用/禁用状态")
    @PutMapping("/admin/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Long currentUserId = UserContext.getUserId();
        if (id.equals(currentUserId)) {
            throw new BusinessException("不能修改当前登录账号自身的状态");
        }
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success(status == 1 ? "用户账号已成功解封启用" : "用户账号已被封禁锁定", null);
    }

    @Operation(summary = "管理端: 分配/变更用户RBAC角色")
    @RequiresRoles({UserRole.ROLE_ADMIN})
    @LogRecord(module = "会员管理", operation = "调整用户RBAC角色")
    @PutMapping("/admin/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || role.trim().isEmpty()) {
            throw new BusinessException("角色标识不能为空");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role.trim());
        user.setUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success("用户角色已成功变更为: " + role, null);
    }

    @Operation(summary = "管理端: 人工调整会员余额")
    @RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
    @LogRecord(module = "会员管理", operation = "人工调整会员账户余额")
    @PostMapping("/admin/users/{id}/balance")
    public Result<SysUser> adjustUserBalance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal adjustAmount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        String reason = (String) body.getOrDefault("reason", "管理员后台人工调整");

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        BigDecimal oldBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal newBalance = oldBalance.add(adjustAmount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("调整后用户余额不能为负数 (当前余额: " + oldBalance + " 元)");
        }

        user.setBalance(newBalance);
        user.setUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);

        // 记入流水账本
        String tradeNo = "ADJ" + System.currentTimeMillis() + (int)((Math.random() * 9 + 1) * 1000);
        PaymentRecord record = PaymentRecord.builder()
                .tradeNo(tradeNo)
                .orderNo("ADMIN-ADJUST-" + System.currentTimeMillis())
                .userId(id)
                .channel("SYSTEM")
                .amount(adjustAmount.abs())
                .payStatus(1)
                .gatewayTradeNo("GATEWAY-" + tradeNo)
                .buyerId("后台调账(" + reason + ")")
                .signType("RSA2")
                .notifyRawData("{\"type\":\"ADMIN_ADJUST\",\"adjustAmount\":" + adjustAmount + ",\"reason\":\"" + reason + "\"}")
                .reconciled(1)
                .createTime(LocalDateTime.now())
                .notifyTime(LocalDateTime.now())
                .build();
        paymentRecordMapper.insert(record);

        return Result.success("余额调整成功，最新余额: " + newBalance + " 元", user);
    }

    @Operation(summary = "管理端: 调整会员信用分")
    @RequiresRoles({UserRole.ROLE_ADMIN, UserRole.ROLE_MANAGER})
    @LogRecord(module = "会员管理", operation = "调整会员履约信用分")
    @PutMapping("/admin/users/{id}/credit")
    public Result<Void> adjustUserCredit(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer creditScore = body.get("creditScore");
        if (creditScore == null || creditScore < 0 || creditScore > 120) {
            throw new BusinessException("信用分取值范围须在 0 ~ 120 之间");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setCreditScore(creditScore);
        user.setUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success("用户信用分已更新为: " + creditScore + " 分", null);
    }

    // ==========================================
    // 3. 内部 VO 数据模型
    // ==========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileVo {
        private Long id;
        private String username;
        private String nickname;
        private String phone;
        private String avatar;
        private String role;
        private BigDecimal balance;
        private Integer creditScore;
        private LocalDateTime createTime;

        private Long totalBookings;
        private Long completedBookings;
        private Long canceledBookings;
        private Long activeBookings;
    }
}
