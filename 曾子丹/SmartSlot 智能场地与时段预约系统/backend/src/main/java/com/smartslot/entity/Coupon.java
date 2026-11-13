package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 营销优惠券模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name; // 优惠券名称，如：新人专享立减券、夜间黄金档满减券

    private String code; // 券批次码

    private Integer type; // 1-满减券, 2-折扣券, 3-无门槛立减券

    private BigDecimal minSpend; // 门槛消费金额(0为无门槛)

    private BigDecimal discountAmount; // 减免金额(针对满减/立减)

    private BigDecimal discountRate; // 折扣比例(如0.85表示85折)

    private Integer validDays; // 领取后有效天数

    private Integer totalCount; // 发放总量

    private Integer claimedCount; // 已被领取数量

    private String description; // 优惠说明

    private Long categoryId; // 限定运动品类ID(null表示全场通用)

    private Integer status; // 1-上架可领, 0-下架停发

    private LocalDateTime createTime;
}
