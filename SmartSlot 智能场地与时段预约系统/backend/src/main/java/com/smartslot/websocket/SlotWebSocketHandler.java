package com.smartslot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartslot.dto.SlotEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时段大屏 WebSocket 长连接处理器
 * 负责客户端会话生命周期维护、心跳保活、以及全网毫秒级广播推送
 */
@Slf4j
@Component
public class SlotWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public SlotWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("[WebSocket] 客户端已成功建立连接: sessionId={}, 当前在线总数={}", session.getId(), sessions.size());
        broadcastOnlineCount();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("PING".equalsIgnoreCase(payload.trim()) || payload.contains("PING")) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\",\"timestamp\":" + System.currentTimeMillis() + "}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("[WebSocket] 客户端已断开连接: sessionId={}, 原因={}, 剩余在线={}", session.getId(), status.getReason(), sessions.size());
        broadcastOnlineCount();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WebSocket] 会话传输异常: sessionId={}, 错误={}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * 全网毫秒级广播时段变更事件
     */
    public void broadcast(SlotEventDto event) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        log.warn("[WebSocket] 消息推送失败，将会话移出池: sessionId={}", session.getId());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
            log.info("[WebSocket全网广播] 事件类型={}, 场地={}, 时段={}, 目标在线端数={}",
                    event.getEventType(), event.getVenueId(), event.getTimeSlot(), sessions.size());
        } catch (Exception e) {
            log.error("[WebSocket] 广播事件序列化或发送异常: ", e);
        }
    }

    /**
     * 广播在线人数
     */
    public void broadcastOnlineCount() {
        SlotEventDto event = SlotEventDto.builder()
                .eventType("ONLINE_COUNT")
                .onlineCount(sessions.size())
                .timestamp(System.currentTimeMillis())
                .build();
        broadcast(event);
    }

    public int getOnlineCount() {
        return sessions.size();
    }
}
