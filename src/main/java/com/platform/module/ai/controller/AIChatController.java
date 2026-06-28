package com.platform.module.ai.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.ai.entity.AIChatMessage;
import com.platform.module.ai.entity.AIChatSession;
import com.platform.module.ai.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService chatService;

    // ==================== 会话管理 ====================

    @PostMapping("/sessions")
    public Result<AIChatSession> createSession() {
        Long userId = UserContext.getCurrentUserId();
        AIChatSession session = chatService.createSession(userId);
        return Result.success(session);
    }

    @GetMapping("/sessions")
    public Result<List<AIChatSession>> getSessions() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(chatService.getSessions(userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = UserContext.getCurrentUserId();
        chatService.deleteSession(sessionId, userId);
        return Result.success();
    }

    // ==================== 消息管理 ====================

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AIChatMessage>> getMessages(@PathVariable Long sessionId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(chatService.getMessages(sessionId, userId));
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getCurrentUserId();
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        String content = body.get("content").toString();
        return Result.success(chatService.sendMessage(userId, sessionId, content));
    }

    /**
     * SSE 流式聊天端点
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getCurrentUserId();
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        String content = body.get("content").toString();

        SseEmitter emitter = new SseEmitter(0L); // 无超时

        chatService.sendMessageStream(userId, sessionId, content,
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

        return emitter;
    }
}
