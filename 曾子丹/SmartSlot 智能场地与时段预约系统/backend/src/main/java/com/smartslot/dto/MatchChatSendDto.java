package com.smartslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送拼场聊天消息 DTO
 */
@Data
public class MatchChatSendDto {

    @NotNull(message = "拼场活动ID不能为空")
    private Long activityId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息长度不能超过 500 个字符")
    private String content;

    /**
     * 消息类型: TEXT-文本, TACTIC-快捷战术短语
     */
    private String msgType = "TEXT";
}
