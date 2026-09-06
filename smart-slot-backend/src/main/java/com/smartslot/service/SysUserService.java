package com.smartslot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.LoginDto;
import com.smartslot.dto.RegisterDto;
import com.smartslot.entity.SysUser;
import com.smartslot.vo.LoginVo;

public interface SysUserService extends IService<SysUser> {

    LoginVo login(LoginDto dto);

    void register(RegisterDto dto);

    SysUser getCurrentUserInfo(Long userId);

    void updateProfile(Long userId, String nickname, String phone);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    SysUser rechargeWallet(Long userId, java.math.BigDecimal amount, String channel);
}
