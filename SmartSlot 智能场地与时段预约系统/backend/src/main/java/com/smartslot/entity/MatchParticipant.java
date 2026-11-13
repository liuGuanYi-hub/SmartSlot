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
 * 拼场参与成员实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("match_participant")
public class MatchParticipant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId; // 关联拼场活动ID

    private Long userId; // 用户ID

    private String username; // 用户名

    private String nickname; // 昵称

    private String avatar; // 头像

    private BigDecimal payAmount; // 支付的 AA 份额金额

    private Integer payStatus; // 1-已支付, 2-已退款

    private Integer isCreator; // 1-发起人, 0-参与成员

    private String verifyCode; // 满员成团后分发的专属到场核销码

    private LocalDateTime joinTime;
}
