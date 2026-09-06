package com.smartslot.service.impl;

import com.smartslot.dto.SlotEventDto;
import com.smartslot.service.WebSocketPushService;
import com.smartslot.websocket.SlotWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 实时推送服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushServiceImpl implements WebSocketPushService {

    private final SlotWebSocketHandler slotWebSocketHandler;

    @Override
    public void broadcastSlotChange(SlotEventDto event) {
        slotWebSocketHandler.broadcast(event);
    }

    @Override
    public void broadcastOnlineCount() {
        slotWebSocketHandler.broadcastOnlineCount();
    }

    @Override
    public int getOnlineCount() {
        return slotWebSocketHandler.getOnlineCount();
    }
}
