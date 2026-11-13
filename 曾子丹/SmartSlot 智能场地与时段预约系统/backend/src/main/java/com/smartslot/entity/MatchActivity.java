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
import java.util.List;

/**
 * 拼场招募活动实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("match_activity")
public class MatchActivity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String activityNo; // 业务招募单号 ACT20260908...

    private Long creatorId; // 发起人ID

    private Long venueId; // 场馆ID

    private String venueName; // 场馆名称

    private String categoryName; // 运动分类名称 (羽毛球, 网球, 篮球等)

    private LocalDate bookDate; // 活动日期

    private String timeSlot; // 活动时段 (09:00-10:00)

    private String title; // 拼场主题

    private String sportTag; // 运动标签 (双打进阶, 新手友好, AA畅打等)

    private Integer targetMembers; // 目标招募人数 (2~10人)

    private Integer currentMembers; // 当前已参与人数

    private BigDecimal totalAmount; // 场地总费用

    private BigDecimal costPerPerson; // 人均 AA 费用

    private String description; // 活动说明与要求

    private Integer status; // 0-招募中, 1-拼场成功待核销, 2-已核销完成, 3-未成团已解散退款

    private LocalDateTime expireTime; // 招募截止时间

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // 冗余发起人信息
    @TableField(exist = false)
    private String creatorName;

    @TableField(exist = false)
    private String creatorAvatar;

    @TableField(exist = false)
    private Integer creatorCreditScore;

    @TableField(exist = false)
    private Boolean isJoined; // 当前登录用户是否已加入

    @TableField(exist = false)
    private String myVerifyCode; // 当前登录用户的专属核销码

    @TableField(exist = false)
    private List<MatchParticipant> participants; // 已加入成员列表
}
