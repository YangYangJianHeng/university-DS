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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 配置
 * 
 * 直接读取项目根目录 .env 文件，不依赖 Spring 的 ${ENV_VAR} 占位符解析，
 * 彻底解决因 PostProcessor 时序问题导致的配置读取失败。
 * 
 * 自动检测策略（v2）：
 *   1. 启动时先检测 Ollama 是否运行
 *   2. 运行中 → 使用 Ollama 本地模型
 *   3. 未运行 → 自动回退到云端 API（如 DeepSeek）
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    /** 内存缓存：Ollama 可用性检测结果（只检测一次） */
    private static volatile Boolean ollamaAvailableCache = null;
    private static volatile String ollamaDetectedModel = null;

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

    // ==================== Ollama 自动检测 ====================

    /**
     * 检测 Ollama 是否运行并包含有效模型
     * 返回 Ollama 运行状态（同时缓存可用模型名称）
     * 结果会缓存，避免每次调用都检测
     */
    private boolean isOllamaRunning() {
        if (ollamaAvailableCache != null) {
            return ollamaAvailableCache;
        }

        Map<String, String> env = loadDotenv();
        String baseUrl = env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String chatModel = env.getOrDefault("OLLAMA_CHAT_MODEL", "qwen2.5:7b");

        try {
            // 1. 检测 Ollama 服务是否响应
            URL url = URI.create(baseUrl + "/api/tags").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                log.warn("Ollama health check failed: HTTP {}", conn.getResponseCode());
                ollamaAvailableCache = false;
                return false;
            }

            // 2. 读取返回体，检查是否有可用模型
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            conn.disconnect();

            String body = sb.toString();
            if (body.isEmpty() || !body.contains("\"models\"")) {
                log.warn("Ollama is running but no models found");
                ollamaAvailableCache = false;
                return false;
            }

            // 3. 解析 JSON 获取可用模型列表
            com.alibaba.fastjson.JSONObject tagsResp = com.alibaba.fastjson.JSON.parseObject(body);
            com.alibaba.fastjson.JSONArray models = tagsResp.getJSONArray("models");
            if (models == null || models.isEmpty()) {
                log.warn("Ollama is running but models list is empty");
                ollamaAvailableCache = false;
                return false;
            }

            // 4. 查找 .env 配置的模型（支持模糊匹配，mymodel → mymodel:latest）
            for (int i = 0; i < models.size(); i++) {
                com.alibaba.fastjson.JSONObject m = models.getJSONObject(i);
                String name = m.getString("name");
                // 完全匹配 或 前缀匹配（mymodel 匹配 mymodel:latest）
                if (chatModel.equals(name) || name.startsWith(chatModel + ":")) {
                    ollamaDetectedModel = name; // 使用 Ollama 完整名称
                    break;
                }
            }

            // 5. 配置的模型未找到 → 自动检测第一个聊天模型（跳过嵌入模型）
            if (ollamaDetectedModel == null) {
                for (int i = 0; i < models.size(); i++) {
                    com.alibaba.fastjson.JSONObject m = models.getJSONObject(i);
                    String name = m.getString("name");
                    String lower = name.toLowerCase();
                    // 跳过嵌入模型（不支持 /api/chat）
                    if (lower.contains("embed") || lower.contains("nomic") || lower.contains("bert")) {
                        log.debug("Skipping embedding model: {}", name);
                        continue;
                    }
                    ollamaDetectedModel = name;
                    log.info("Ollama model '{}' not found, auto-detected chat model: '{}'",
                            chatModel, ollamaDetectedModel);
                    break;
                }
            }

            if (ollamaDetectedModel == null || ollamaDetectedModel.isEmpty()) {
                log.warn("Ollama running but unable to detect model name");
                ollamaAvailableCache = false;
                return false;
            }

            log.info("✅ Ollama detected: baseUrl={}, model={}", baseUrl, ollamaDetectedModel);
            ollamaAvailableCache = true;
            return true;

        } catch (Exception e) {
            log.info("Ollama not available ({}), will use cloud API", e.getMessage());
            ollamaAvailableCache = false;
            return false;
        }
    }

    /**
     * 获取检测到的 Ollama 模型名称（仅在 isOllamaRunning() 调用后有效）
     */
    private String getOllamaModel() {
        return ollamaDetectedModel != null ? ollamaDetectedModel : "qwen2.5:7b";
    }

    // ==================== ChatLanguageModel Bean ====================

    /**
     * 创建聊天模型 Bean（自动检测 Ollama 可用性）
     * 优先使用本地 Ollama → 不可用时回退到云端 API
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        Map<String, String> env = loadDotenv();

        // —— 自动检测 Ollama ——
        if (isOllamaRunning()) {
            String baseUrl = env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            String model = getOllamaModel();
            int timeout = Integer.parseInt(env.getOrDefault("OLLAMA_TIMEOUT_SECONDS", "120"));
            log.info("🟢 [LangChain4j] Auto-detected Ollama → using OllamaChatModel: baseUrl={}, model={}", baseUrl, model);
            return OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeout))
                    .build();
        }

        // —— Ollama 不可用，回退到云端 API ——
        String baseUrl = env.getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
        String apiKey = env.getOrDefault("OPENAI_API_KEY", "");
        String model = env.getOrDefault("OPENAI_CHAT_MODEL", "deepseek-v4-flash");

        if (apiKey.isEmpty()) {
            log.error("!!! OPENAI_API_KEY is EMPTY! Chat will fail.");
        } else {
            String masked = apiKey.length() <= 10 ? "***" : apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
            log.info("🔵 [LangChain4j] Ollama not available → fallback to OpenAiChatModel: baseUrl={}, model={}", baseUrl, model);
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

    /**
     * 创建嵌入模型 Bean（自动检测 Ollama 可用性）
     * 嵌入优先用 Ollama 本地（速度快、免费），不可用时回退云端
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        Map<String, String> env = loadDotenv();

        // —— 自动检测 Ollama ——
        if (isOllamaRunning()) {
            String baseUrl = env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            String model = env.getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text");
            int timeout = Integer.parseInt(env.getOrDefault("OLLAMA_TIMEOUT_SECONDS", "120"));
            log.info("🟢 [LangChain4j] Auto-detected Ollama → using OllamaEmbeddingModel: baseUrl={}, model={}", baseUrl, model);
            return OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeout))
                    .build();
        }

        // —— Ollama 不可用，回退到 OpenAI 兼容 Embedding ——
        String baseUrl = env.getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
        String apiKey = env.getOrDefault("OPENAI_API_KEY", "");
        String model = env.getOrDefault("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small");

        // DeepSeek 不支持 embedding，如果 baseUrl 是 DeepSeek 则跳过 → 回退到内存
        if (baseUrl.contains("deepseek.com")) {
            log.warn("🔴 DeepSeek does not support embeddings. "
                    + "Install Ollama with 'nomic-embed-text' for RAG. "
                    + "RAG will fallback to in-memory mode.");
            return null;
        }

        log.info("🔵 [LangChain4j] Ollama not available → fallback to OpenAIEmbeddingModel: baseUrl={}, model={}", baseUrl, model);
        return dev.langchain4j.model.openai.OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
