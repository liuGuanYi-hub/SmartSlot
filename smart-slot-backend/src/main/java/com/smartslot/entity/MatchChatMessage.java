package com.smartslot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 拼场搭子实时聊天消息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("match_chat_message")
public class MatchChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的拼场活动ID
     */
    private Long activityId;

    /**
     * 发送人用户ID
     */
    private Long userId;

    /**
     * 发送人用户名
     */
    private String username;

    /**
     * 发送人昵称
     */
    private String nickname;

    /**
     * 发送人头像
     */
    private String avatar;

    /**
     * 是否为拼场发起人: 1-是, 0-否
     */
    private Integer isCreator;

    /**
     * 消息正文 (支持文本与快捷战术短语)
     */
    private String content;

    /**
     * 消息类型: TEXT-普通文本, TACTIC-快捷战术装备, SYSTEM-系统成团/加入广播
     */
    private String msgType;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;
}
