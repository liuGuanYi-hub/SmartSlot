package com.smartslot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拼场搭子微聊室 WebSocket 长连接处理器
 * 支持基于 activityId 的房间级会话隔离与组播分发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /**
     * 房间会话映射: activityId -> Set<WebSocketSession>
     */
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long activityId = extractActivityId(session);
        if (activityId == null) {
            log.warn("[搭子微聊] 连接未携带有效的 activityId，关闭连接: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put("activityId", activityId);
        Set<WebSocketSession> sessions = roomSessions.computeIfAbsent(activityId, k -> ConcurrentHashMap.newKeySet());
        sessions.add(session);

        log.info("[搭子微聊] 球友已进入微室: activityId={}, sessionId={}, 当前微室在线人数={}",
                activityId, session.getId(), sessions.size());

        // 广播房间最新在线搭子人数
        broadcastRoomOnlineCount(activityId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("PING".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("PONG"));
            return;
        }
        // 消息收发主要由 REST API 统一落库校验并调用 broadcastToRoom 分发，
        // 此处保留 WebSocket 直连心跳与双工透传支持
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long activityId = (Long) session.getAttributes().get("activityId");
        if (activityId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(activityId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(activityId);
                } else {
                    broadcastRoomOnlineCount(activityId);
                }
            }
            log.info("[搭子微聊] 球友已离开微室: activityId={}, sessionId={}, 剩余在线={}",
                    activityId, session.getId(), sessions != null ? sessions.size() : 0);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[搭子微聊] 会话传输异常: sessionId={}, 错误={}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 向指定拼场活动房间广播消息 (例如新发言、战术短语、成团通知)
     */
    public void broadcastToRoom(Long activityId, Object messageObject) {
        Set<WebSocketSession> sessions = roomSessions.get(activityId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("[搭子微聊] 房间 activityId={} 当前无在线监听端，消息仅落库", activityId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(messageObject);
            TextMessage textMessage = new TextMessage(json);
            List<WebSocketSession> deadSessions = new ArrayList<>();

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try {
                        synchronized (s) {
                            s.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        log.warn("[搭子微聊] 推送失败，标记失效会话: sessionId={}", s.getId());
                        deadSessions.add(s);
                    }
                } else {
                    deadSessions.add(s);
                }
            }
            sessions.removeAll(deadSessions);
        } catch (Exception e) {
            log.error("[搭子微聊] 消息广播序列化异常: activityId={}", activityId, e);
        }
    }

    /**
     * 广播房间实时在线搭子人数
     */
    private void broadcastRoomOnlineCount(Long activityId) {
        Set<WebSocketSession> sessions = roomSessions.get(activityId);
        int count = sessions != null ? sessions.size() : 0;
        Map<String, Object> map = new HashMap<>();
        map.put("eventType", "ROOM_ONLINE_COUNT");
        map.put("activityId", activityId);
        map.put("onlineCount", count);
        broadcastToRoom(activityId, map);
    }

    /**
     * 从连接 URL 中解析 activityId 参数
     * 如 /ws/match-chat?activityId=3
     */
    private Long extractActivityId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String param : uri.getQuery().split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "activityId".equalsIgnoreCase(pair[0])) {
                try {
                    return Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
