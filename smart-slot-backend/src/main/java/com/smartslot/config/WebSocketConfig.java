package com.smartslot.config;

import com.smartslot.websocket.MatchChatWebSocketHandler;
import com.smartslot.websocket.SlotWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置中心
 * 1. 注册全网时段协同广播长连接通道 /ws/slot
 * 2. 注册拼场搭子微聊室长连接通道 /ws/match-chat
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SlotWebSocketHandler slotWebSocketHandler;
    private final MatchChatWebSocketHandler matchChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(slotWebSocketHandler, "/ws/slot")
                .setAllowedOrigins("*");

        registry.addHandler(matchChatWebSocketHandler, "/ws/match-chat")
                .setAllowedOrigins("*");
    }
}

