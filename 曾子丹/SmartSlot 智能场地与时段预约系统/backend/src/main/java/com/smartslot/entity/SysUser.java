package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String phone;

    private String avatar;

    private String role; // ROLE_USER, ROLE_ADMIN

    private BigDecimal balance;

    private Integer status; // 1-启用, 0-禁用

    /**
     * 履约信用分 (默认 100，最高 120，低于 70 限制预订高峰时段)
     */
    @Builder.Default
    private Integer creditScore = 100;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
