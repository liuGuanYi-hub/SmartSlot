package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户领券记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long couponId;

    private Long userId;

    private Integer status; // 0-未使用, 1-已使用, 2-已过期

    private LocalDateTime claimTime;

    private LocalDateTime expireTime;

    private LocalDateTime usedTime;

    private String orderNo;

    // 冗余字段方便前端渲染
    @TableField(exist = false)
    private String couponName;

    @TableField(exist = false)
    private Integer couponType;

    @TableField(exist = false)
    private BigDecimal minSpend;

    @TableField(exist = false)
    private BigDecimal discountAmount;

    @TableField(exist = false)
    private BigDecimal discountRate;

    @TableField(exist = false)
    private String description;
}
