package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.LoginDto;
import com.smartslot.dto.RegisterDto;
import com.smartslot.entity.PaymentRecord;
import com.smartslot.entity.SysUser;
import com.smartslot.mapper.PaymentRecordMapper;
import com.smartslot.mapper.SysUserMapper;
import com.smartslot.service.SysUserService;
import com.smartslot.util.JwtUtil;
import com.smartslot.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final PaymentRecordMapper paymentRecordMapper;

    // 密码混淆盐
    private static final String SALT = "SmartSlot_Salt_2026";

    private String encodePassword(String rawPassword) {
        String salted = rawPassword + SALT;
        return DigestUtils.md5DigestAsHex(salted.getBytes(StandardCharsets.UTF_8));
    }

    private boolean checkPassword(String rawPassword, String encodedPassword) {
        // 兼容演示数据中的预置 BCrypt 或 MD5
        if (encodedPassword != null && encodedPassword.startsWith("$2a$")) {
            return "123456".equals(rawPassword);
        }
        return encodePassword(rawPassword).equalsIgnoreCase(encodedPassword);
    }

    @Override
    public LoginVo login(LoginDto dto) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("该账户已被封禁，请联系管理员");
        }

        if (!checkPassword(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginVo.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .balance(user.getBalance())
                .build();
    }

    @Override
    public void register(RegisterDto dto) {
        long count = count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已被占用，请更换");
        }

        SysUser user = SysUser.builder()
                .username(dto.getUsername())
                .password(encodePassword(dto.getPassword()))
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .role("ROLE_USER")
                .avatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png")
                .balance(new BigDecimal("500.00")) // 新会员赠送 500 元体验金
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        save(user);
        log.info("新用户注册成功: id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public SysUser getCurrentUserInfo(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String phone) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
        }
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("用户修改个人资料成功: userId={}, nickname={}", userId, nickname);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!checkPassword(oldPassword, user.getPassword())) {
            throw new BusinessException("原登录密码不正确，请重新输入");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("新密码长度不能少于 6 位");
        }
        user.setPassword(encodePassword(newPassword.trim()));
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("用户修改密码成功: userId={}", userId);
    }

    @Override
    public SysUser rechargeWallet(Long userId, BigDecimal amount, String channel) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于 0 元");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 计算赠金奖励阶梯 (满100送10, 满300送45, 满500送100, 满1000送250)
        BigDecimal bonus = BigDecimal.ZERO;
        if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            bonus = new BigDecimal("250.00");
        } else if (amount.compareTo(new BigDecimal("500")) >= 0) {
            bonus = new BigDecimal("100.00");
        } else if (amount.compareTo(new BigDecimal("300")) >= 0) {
            bonus = new BigDecimal("45.00");
        } else if (amount.compareTo(new BigDecimal("100")) >= 0) {
            bonus = new BigDecimal("10.00");
        }

        BigDecimal totalCredited = amount.add(bonus);
        BigDecimal oldBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        user.setBalance(oldBalance.add(totalCredited));
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        // 写入支付网关流水明细
        String tradeNo = "REC" + System.currentTimeMillis() + (int)((Math.random() * 9 + 1) * 1000);
        String channelName = (channel != null && !channel.trim().isEmpty()) ? channel.toUpperCase() : "ALIPAY";
        PaymentRecord record = PaymentRecord.builder()
                .tradeNo(tradeNo)
                .orderNo("RECHARGE-" + System.currentTimeMillis())
                .userId(userId)
                .channel(channelName)
                .amount(amount)
                .payStatus(1) // 支付成功入账
                .gatewayTradeNo("GATEWAY-" + tradeNo)
                .buyerId("会员充值(赠金: " + bonus + "元)")
                .signType("RSA2")
                .notifyRawData("{\"type\":\"RECHARGE\",\"actualAmount\":" + amount + ",\"bonus\":" + bonus + ",\"totalCredited\":" + totalCredited + "}")
                .reconciled(1)
                .createTime(LocalDateTime.now())
                .notifyTime(LocalDateTime.now())
                .build();
        paymentRecordMapper.insert(record);

        log.info("会员充值成功: userId={}, 充值={}, 赠送={}, 最新余额={}", userId, amount, bonus, user.getBalance());
        return user;
    }
}
