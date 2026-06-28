package com.platform.module.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 LangChain4j 的简单问答智能体（仅同步）
 * 流式调用仍使用原始的 OpenAIClient/OllamaClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jAgentService {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * 同步对话
     */
    public String chat(String systemPrompt,
                       List<java.util.Map<String, String>> history,
                       String userMessage) {
        try {
            List<ChatMessage> messages = buildMessages(systemPrompt, history, userMessage);
            ChatResponse response = chatLanguageModel.chat(ChatRequest.builder().messages(messages).build());
            return response.aiMessage().text();
        } catch (Exception e) {
            log.error("LangChain4j chat error: {}", e.getMessage());
            return "抱歉，AI 服务调用失败：" + e.getMessage();
        }
    }

    /**
     * 构建 LangChain4j 消息列表
     */
    private List<ChatMessage> buildMessages(String systemPrompt,
                                            List<java.util.Map<String, String>> history,
                                            String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(SystemMessage.from(systemPrompt));
        }

        if (history != null) {
            for (java.util.Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if (content == null || content.isEmpty()) continue;

                switch (role != null ? role : "") {
                    case "user" -> messages.add(UserMessage.from(content));
                    case "system" -> messages.add(SystemMessage.from(content));
                    default -> messages.add(AiMessage.from(content));
                }
            }
        }

        messages.add(UserMessage.from(userMessage));
        return messages;
    }
}
