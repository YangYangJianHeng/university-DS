package com.platform.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.module.ai.entity.AIChatMessage;
import com.platform.module.ai.entity.AIChatSession;
import com.platform.module.ai.mapper.AIChatMessageMapper;
import com.platform.module.ai.mapper.AIChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天服务层
 * 对外暴露：创建会话、发送消息、获取历史等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final AIChatSessionMapper sessionMapper;
    private final AIChatMessageMapper messageMapper;
    private final AiAgentService agentService;

    /**
     * 创建新会话
     */
    public AIChatSession createSession(Long userId) {
        AIChatSession session = new AIChatSession();
        session.setUserId(userId);
        session.setTitle("新的对话");
        session.setMessageCount(0);
        session.setStatus("ACTIVE");
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 获取用户的会话列表
     */
    public List<AIChatSession> getSessions(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AIChatSession>()
                        .eq(AIChatSession::getUserId, userId)
                        .orderByDesc(AIChatSession::getUpdatedAt));
    }

    /**
     * 获取会话的所有消息
     */
    public List<AIChatMessage> getMessages(Long sessionId, Long userId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .eq(AIChatMessage::getUserId, userId)
                        .eq(AIChatMessage::getCompressed, 0)
                        .orderByAsc(AIChatMessage::getCreatedAt));
    }

    /**
     * 发送消息并获取回复（同步）
     */
    public Map<String, Object> sendMessage(Long userId, Long sessionId, String content) {
        // 验证会话归属
        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        String response = agentService.processMessage(userId, sessionId, content);

        return Map.of(
                "role", "assistant",
                "content", response,
                "sessionId", sessionId
        );
    }

    /**
     * 发送消息并获取流式回复
     */
    public void sendMessageStream(Long userId, Long sessionId, String content,
                                   java.util.function.Consumer<String> onChunk,
                                   Runnable onDone) {
        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            onChunk.accept("会话不存在或无权访问");
            onDone.run();
            return;
        }
        agentService.processMessageStream(userId, sessionId, content, onChunk, onDone);
    }

    /**
     * 删除会话
     */
    public void deleteSession(Long sessionId, Long userId) {
        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        // 删除消息
        messageMapper.delete(new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getSessionId, sessionId));
        // 删除会话
        sessionMapper.deleteById(sessionId);
    }
}
