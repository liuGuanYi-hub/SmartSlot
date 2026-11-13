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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("booking_order")
public class BookingOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long venueId;

    private LocalDate bookDate;

    private String timeSlot; // "09:00-10:00"

    private BigDecimal totalAmount;

    private Long couponId; // 关联使用的优惠券ID

    private BigDecimal discountAmount; // 优惠券抵扣金额

    private BigDecimal actualAmount; // 券后实付金额

    private Integer payStatus; // 0-未支付, 1-已支付, 2-已退款

    private Integer orderStatus; // 0-待支付锁定中, 1-预约成功(待核销), 2-已完成(已核销), 3-已取消

    private String verifyCode; // 6位专属核销码

    private String contactName;

    private String contactPhone;

    private LocalDateTime payTime;

    private LocalDateTime verifyTime;

    private LocalDateTime expireTime;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String venueName;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String username;
}
