package com.platform.module.collaboration.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/ws/chat/{teamId}/{userId}")
public class ChatWebSocket {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocket.class);

    private static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Set<String>> teamSessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("teamId") Long teamId, @PathParam("userId") Long userId) {
        String sessionId = teamId + ":" + userId + ":" + session.getId();
        sessionMap.put(sessionId, session);

        teamSessionMap.computeIfAbsent(teamId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        // Notify others that user came online
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "SYSTEM");
        msg.put("content", "用户 " + userId + " 加入了聊天室");
        msg.put("userId", userId);
        msg.put("timestamp", LocalDateTime.now().toString());
        broadcast(teamId, msg);

        log.info("WebSocket connected: teamId={}, userId={}, sessionId={}", teamId, userId, sessionId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("teamId") Long teamId, @PathParam("userId") Long userId) {
        try {
            JSONObject json = JSON.parseObject(message);
            String content = json.getString("content");
            String contentType = json.getString("contentType");
            if (contentType == null) {
                contentType = "TEXT";
            }

            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "CHAT");
            msg.put("content", content);
            msg.put("contentType", contentType);
            msg.put("userId", userId);
            msg.put("teamId", teamId);
            msg.put("timestamp", LocalDateTime.now().toString());

            broadcast(teamId, msg);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("teamId") Long teamId, @PathParam("userId") Long userId) {
        String sessionId = teamId + ":" + userId + ":" + session.getId();
        sessionMap.remove(sessionId);

        Set<String> sessions = teamSessionMap.get(teamId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                teamSessionMap.remove(teamId);
            }
        }

        // Notify others that user went offline
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "SYSTEM");
        msg.put("content", "用户 " + userId + " 离开了聊天室");
        msg.put("userId", userId);
        msg.put("timestamp", LocalDateTime.now().toString());
        broadcast(teamId, msg);

        log.info("WebSocket disconnected: teamId={}, userId={}, sessionId={}", teamId, userId, sessionId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error: {}", error.getMessage());
    }

    private void broadcast(Long teamId, Object message) {
        Set<String> sessions = teamSessionMap.get(teamId);
        if (sessions == null) return;

        String jsonStr = JSON.toJSONString(message);
        for (String sessionId : sessions) {
            Session session = sessionMap.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(jsonStr);
                } catch (Exception e) {
                    log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }
}
