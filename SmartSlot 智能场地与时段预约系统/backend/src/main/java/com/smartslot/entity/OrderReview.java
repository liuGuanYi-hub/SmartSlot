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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_review")
public class OrderReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long venueId;

    private Long userId;

    private Integer rating; // 综合评分 1-5星

    private Integer envRating; // 环境打分 1-5

    private Integer facilityRating; // 设施打分 1-5

    private Integer serviceRating; // 服务打分 1-5

    private String tags; // 评价标签(如: 奥运专业地胶,防眩光灯光)

    private String images; // 实拍晒图URL集合(逗号分隔或JSON)

    private String merchantReply; // 商家/店长官方回复

    private Integer likes; // 觉得有用/点赞数

    private String content; // 评价详细心得评语

    private LocalDateTime createTime;

    @TableField(exist = false)
    private String userNickname;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String userRole; // 用户身份角色

    @TableField(exist = false)
    private String venueName; // 场馆名称
}
