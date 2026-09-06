package com.smartslot.service;

import com.smartslot.dto.SlotEventDto;

/**
 * WebSocket 实时推送服务接口
 */
public interface WebSocketPushService {

    /**
     * 广播时段状态变更事件给所有在线客户端
     */
    void broadcastSlotChange(SlotEventDto event);

    /**
     * 广播当前实时在线协同人数
     */
    void broadcastOnlineCount();

    /**
     * 获取当前在线连接数
     */
    int getOnlineCount();
}
