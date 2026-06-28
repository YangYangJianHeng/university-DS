package com.platform.module.ai.service;

import com.platform.module.ai.config.AiProviderConfig;
import com.platform.module.ai.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Embedding 路由服务
 * 根据 AI_EMBED_PROVIDER 自动选择嵌入模型（独立于聊天提供商）
 * DeepSeek 无 embedding API，自动跳过
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiProviderConfig providerConfig;
    private final OpenAIConfig openAIConfig;
    private final OllamaClient ollamaClient;
    private final OpenAIClient openAIClient;

    /** 上次打印 "All embedding providers failed" 的时间戳，避免日志轰炸 */
    private volatile long lastFailLogTime = 0;
    private static final long FAIL_LOG_INTERVAL_MS = 60_000; // 60秒内只报一次

    /**
     * 获取文本向量，根据 AI_EMBED_PROVIDER 路由
     * DeepSeek 自动跳过（不支持 /v1/embeddings）
     */
    public float[] embed(String text) {
        String embedProvider = getEffectiveEmbedProvider();

        if ("openai".equalsIgnoreCase(embedProvider)) {
            // DeepSeek 不支持 embedding，直接跳过
            if (isDeepSeek()) {
                float[] vec = ollamaClient.embed(text);
                if (vec.length > 0) return vec;
            } else {
                // 原生 OpenAI embedding
                float[] vec = openAIClient.embed(text);
                if (vec.length > 0) return vec;
                vec = ollamaClient.embed(text);
                if (vec.length > 0) return vec;
            }
        } else {
            // 首选 Ollama
            float[] vec = ollamaClient.embed(text);
            if (vec.length > 0) return vec;

            // Ollama 失败，尝试 OpenAI（DeepSeek 除外）
            if (!isDeepSeek()) {
                vec = openAIClient.embed(text);
                if (vec.length > 0) return vec;
            }
        }

        // 节流错误日志：60秒内只打印一次，避免每篇文档都刷屏
        long now = System.currentTimeMillis();
        if (now - lastFailLogTime > FAIL_LOG_INTERVAL_MS) {
            lastFailLogTime = now;
            log.error("Embedding unavailable: all providers failed. "
                    + "Check if Ollama (embed={}) or OpenAI embedding service is running.",
                    getEffectiveEmbedProvider());
        }
        return new float[0];
    }

    /**
     * 获取有效的 embedding 提供商
     * 如果 AI_EMBED_PROVIDER 未设置，默认跟随 AI_PROVIDER
     */
    private String getEffectiveEmbedProvider() {
        String embedProvider = providerConfig.getEmbedProvider();
        if (embedProvider == null || embedProvider.isEmpty() || "ollama".equals(embedProvider)) {
            return embedProvider == null || embedProvider.isEmpty() ? "ollama" : embedProvider;
        }
        // 如果 embedProvider 有非默认值则使用它
        // 默认值是 ollama，如果没设置 AI_EMBED_PROVIDER 则尝试跟随 AI_PROVIDER
        if ("ollama".equals(embedProvider) && !"ollama".equals(providerConfig.getProvider())) {
            // embedProvider 是默认 ollama，但 provider 不是 ollama → 跟随 provider
            return providerConfig.getProvider();
        }
        return embedProvider;
    }

    /** 检测 OpenAI base URL 是否指向 DeepSeek（其不支持 /v1/embeddings） */
    private boolean isDeepSeek() {
        String baseUrl = openAIConfig.getBaseUrl();
        return baseUrl != null && baseUrl.contains("deepseek.com");
    }
}
