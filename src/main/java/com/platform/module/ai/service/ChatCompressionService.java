package com.platform.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.module.ai.config.ChatConfig;
import com.platform.module.ai.entity.AIChatMessage;
import com.platform.module.ai.entity.AIChatSession;
import com.platform.module.ai.mapper.AIChatMessageMapper;
import com.platform.module.ai.mapper.AIChatSessionMapper;
import com.platform.module.ai.config.OllamaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天记录压缩服务
 * 当对话轮次超过阈值时，使用 LLM 将较早的消息压缩为摘要
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCompressionService {

    private final ChatConfig chatConfig;
    private final OllamaClient ollamaClient;
    private final OllamaConfig ollamaConfig;
    private final AIChatSessionMapper sessionMapper;
    private final AIChatMessageMapper messageMapper;

    /**
     * 检查并执行压缩
     * @return 压缩后的消息列表（最近N条完整 + 摘要作为系统上下文）
     */
    public List<Map<String, String>> checkAndCompress(Long sessionId, Long userId) {
        // 1. 获取所有未压缩的消息
        List<AIChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .eq(AIChatMessage::getCompressed, 0)
                        .orderByAsc(AIChatMessage::getCreatedAt));

        int totalMessages = messages.size();
        if (totalMessages <= chatConfig.getCompressTrigger()) {
            // 未达到压缩阈值，返回全部消息
            return messages.stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                    .collect(Collectors.toList());
        }

        // 2. 需要压缩：分割为旧消息和保留消息
        int keepCount = chatConfig.getCompressKeepRecent() * 2; // 最近N轮 = N*2条
        List<AIChatMessage> oldMessages = messages.subList(0, totalMessages - keepCount);
        List<AIChatMessage> recentMessages = messages.subList(totalMessages - keepCount, totalMessages);

        // 3. 调用 LLM 压缩旧消息为摘要
        String summary = generateSummary(oldMessages);
        log.info("Compressed {} messages into summary ({} chars), keeping {} recent messages",
                oldMessages.size(), summary.length(), recentMessages.size());

        // 4. 更新数据库：标记旧消息为已压缩，更新会话摘要
        List<Long> oldIds = oldMessages.stream().map(AIChatMessage::getId).collect(Collectors.toList());
        if (!oldIds.isEmpty()) {
            messageMapper.update(null,
                    new LambdaUpdateWrapper<AIChatMessage>()
                            .in(AIChatMessage::getId, oldIds)
                            .set(AIChatMessage::getCompressed, 1));
        }

        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setSummary(summary);
            sessionMapper.updateById(session);
        }

        // 5. 构建返回的消息列表：摘要作为第一条 system 消息 + 最近的消息
        List<Map<String, String>> result = new ArrayList<>();
        result.add(Map.of("role", "system", "content", "以下是对话历史的摘要，请在回答时参考：\n" + summary));

        for (AIChatMessage msg : recentMessages) {
            result.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        return result;
    }

    /**
     * 使用 LLM 生成对话摘要
     */
    private String generateSummary(List<AIChatMessage> messages) {
        StringBuilder conversation = new StringBuilder();
        for (AIChatMessage msg : messages) {
            conversation.append(msg.getRole().equals("user") ? "用户：" : "助手：")
                    .append(msg.getContent()).append("\n");
        }

        List<Map<String, String>> summaryMessages = List.of(
                Map.of("role", "system", "content",
                        "请将以下对话历史压缩为一段简短的摘要（200字以内），保留关键话题、用户关注的重点和重要结论。只输出摘要文本。"),
                Map.of("role", "user", "content", conversation.toString())
        );

        try {
            String summary = ollamaClient.chat(ollamaConfig.getChatModel(), summaryMessages);
            return summary != null ? summary : "对话历史摘要（自动生成）";
        } catch (Exception e) {
            log.warn("Summary generation failed: {}", e.getMessage());
            return "对话历史摘要（自动生成）";
        }
    }

    /**
     * 估算文本 token 数（中文按字数，英文按空格分词）
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int englishWords = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            }
        }
        englishWords = text.replaceAll("[\\u4e00-\\u9fff]", " ").split("\\s+").length;
        return chineseChars + englishWords;
    }
}
