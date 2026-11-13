package com.smartslot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.MatchChatSendDto;
import com.smartslot.entity.MatchChatMessage;

import java.util.List;

/**
 * 拼场搭子实时微聊服务接口
 */
public interface MatchChatService extends IService<MatchChatMessage> {

    /**
     * 查询指定拼场活动的历史聊天记录
     *
     * @param activityId 拼场活动ID
     * @param userId     当前请求用户ID (用于身份与访问权限校验)
     * @return 历史消息列表 (按时间正序)
     */
    List<MatchChatMessage> listHistory(Long activityId, Long userId);

    /**
     * 发送聊天消息并落库，同时通过 WebSocket 广播给该拼场房间下的所有在线球友
     *
     * @param dto    发送入参
     * @param userId 发送人用户ID
     * @return 保存后的消息对象
     */
    MatchChatMessage sendAndBroadcast(MatchChatSendDto dto, Long userId);
}
