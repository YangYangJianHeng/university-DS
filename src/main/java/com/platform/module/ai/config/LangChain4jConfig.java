package com.platform.module.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 配置
 * 
 * 直接读取项目根目录 .env 文件，不依赖 Spring 的 ${ENV_VAR} 占位符解析，
 * 彻底解决因 PostProcessor 时序问题导致的配置读取失败。
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    /**
     * 读取 .env 文件为 Map
     */
    private Map<String, String> loadDotenv() {
        Map<String, String> env = new HashMap<>();
        File envFile = findEnvFile();
        if (envFile == null || !envFile.exists()) {
            log.warn(".env file not found, using defaults");
            return env;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) continue;
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                // 去除引号
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    env.put(key, value);
                }
            }
            log.info("[LangChain4j] Loaded {} variables from {}", env.size(), envFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("[LangChain4j] Failed to load .env: {}", e.getMessage());
        }
        return env;
    }

    private File findEnvFile() {
        // 1. user.dir
        File f = new File(System.getProperty("user.dir"), ".env");
        if (f.exists()) return f;
        // 2. 当前目录
        f = new File(".env").getAbsoluteFile();
        if (f.exists()) return f;
        // 3. 向上查找
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File env = new File(current, ".env");
            if (env.exists()) return env;
            current = current.getParentFile();
        }
        return new File(System.getProperty("user.dir"), ".env");
    }

    // ==================== ChatLanguageModel Bean ====================

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        Map<String, String> env = loadDotenv();

        String provider = env.getOrDefault("AI_PROVIDER", "openai");

        if ("ollama".equalsIgnoreCase(provider)) {
            String baseUrl = env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            String model = env.getOrDefault("OLLAMA_CHAT_MODEL", "qwen2.5:7b");
            int timeout = Integer.parseInt(env.getOrDefault("OLLAMA_TIMEOUT_SECONDS", "120"));

            log.info("[LangChain4j] Creating OllamaChatModel: baseUrl={}, model={}", baseUrl, model);
            return OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeout))
                    .build();
        }

        // 默认 OpenAI 兼容（DeepSeek 等）
        String baseUrl = env.getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
        String apiKey = env.getOrDefault("OPENAI_API_KEY", "");
        String model = env.getOrDefault("OPENAI_CHAT_MODEL", "deepseek-v4-flash");

        if (apiKey.isEmpty()) {
            log.error("!!! OPENAI_API_KEY is EMPTY in .env! Chat will fail with 401.");
        } else {
            String masked = apiKey.length() <= 10 ? "***" : apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
            log.info("[LangChain4j] Creating OpenAiChatModel: baseUrl={}, apiKey={}, model={}", baseUrl, masked, model);
        }

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .build();
    }

    // ==================== Streaming ChatLanguageModel Bean ====================

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        Map<String, String> env = loadDotenv();

        String baseUrl = env.getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
        String apiKey = env.getOrDefault("OPENAI_API_KEY", "");
        String model = env.getOrDefault("OPENAI_CHAT_MODEL", "deepseek-v4-flash");

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    // ==================== EmbeddingModel Bean ====================

    @Bean
    public EmbeddingModel embeddingModel() {
        Map<String, String> env = loadDotenv();

        String embedProvider = env.getOrDefault("AI_EMBED_PROVIDER", "ollama");

        if ("openai".equalsIgnoreCase(embedProvider)) {
            String baseUrl = env.getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
            String apiKey = env.getOrDefault("OPENAI_API_KEY", "");
            String model = env.getOrDefault("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small");
            log.info("[LangChain4j] Creating OpenAI EmbeddingModel: baseUrl={}, model={}", baseUrl, model);
            return dev.langchain4j.model.openai.OpenAiEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        // 默认 Ollama embedding
        String baseUrl = env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String model = env.getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text");
        int timeout = Integer.parseInt(env.getOrDefault("OLLAMA_TIMEOUT_SECONDS", "120"));

        log.info("[LangChain4j] Creating OllamaEmbeddingModel: baseUrl={}, model={}", baseUrl, model);
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }
}
