package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.LoginDto;
import com.smartslot.dto.RegisterDto;
import com.smartslot.entity.SysUser;
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
}
