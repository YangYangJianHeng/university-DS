package com.platform.module.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.platform.module.ai.config.OllamaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ollama HTTP API 客户端
 * 直接调用 Ollama REST API（/api/chat, /api/embeddings）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaClient {

    private final OllamaConfig config;

    /** 上次打印 embed 错误的时间戳，避免日志轰炸 */
    private volatile long lastEmbedFailLogTime = 0;
    private static final long FAIL_LOG_INTERVAL_MS = 60_000; // 60秒内只报一次

    /** 自动检测到的有效模型名称（优先使用 .env 配置的，否则取 Ollama 中第一个可用模型） */
    private volatile String detectedChatModel = null;

    // ==================== Chat API ====================

    /**
     * 调用 Ollama Chat API（同步，返回完整回复）
     */
    public String chat(String model, List<Map<String, String>> messages) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", false);

            JSONObject resp = postJson("/api/chat", body);
            if (resp == null) return "抱歉，AI 服务暂时不可用，请稍后再试。";

            JSONObject msg = resp.getJSONObject("message");
            return msg != null ? msg.getString("content") : resp.getString("response");
        } catch (Exception e) {
            log.error("Ollama chat error: {}", e.getMessage());
            return "抱歉，AI 服务调用失败：" + e.getMessage();
        }
    }

    /**
     * 调用 Ollama Chat API（流式，逐块回调）
     * 自动检测模型名称
     */
    public void chatStream(String model, List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onDone) {
        String effectiveModel = getEffectiveChatModel(model);
        try {
            JSONObject body = new JSONObject();
            body.put("model", effectiveModel);
            body.put("messages", messages);
            body.put("stream", true);

            postJsonStream("/api/chat", body, line -> {
                if (line == null || line.isEmpty()) return;
                try {
                    JSONObject chunk = JSON.parseObject(line);
                    if (chunk.getBoolean("done")) {
                        onDone.run();
                        return;
                    }
                    JSONObject msg = chunk.getJSONObject("message");
                    if (msg != null && msg.containsKey("content")) {
                        onChunk.accept(msg.getString("content"));
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            log.error("Ollama stream error: {}", e.getMessage());
            onChunk.accept("抱歉，AI 服务调用失败：" + e.getMessage());
            onDone.run();
        }
    }

    // ==================== 健康检测 ====================

    /**
     * 获取有效的聊天模型名称
     * 优先使用 .env 配置的模型 → 配置的不可用则自动检测 Ollama 中第一个聊天模型
     * 关键：自动检测时跳过嵌入模型（nomic-embed-text 等），只选聊天模型
     */
    private String getEffectiveChatModel(String configuredModel) {
        if (detectedChatModel != null) {
            return detectedChatModel;
        }
        synchronized (this) {
            if (detectedChatModel != null) {
                return detectedChatModel;
            }
            // 1. 先尝试解析配置的模型名 → 获取 Ollama 中的完整名称（如 mymodel → mymodel:latest）
            String resolved = resolveModelName(configuredModel);
            if (isModelAvailable(configuredModel)) {
                detectedChatModel = resolved;
                log.info("Ollama chat model: {} (from .env, resolved to '{}')", configuredModel, detectedChatModel);
                return detectedChatModel;
            }
            // 2. 配置的模型不可用 → 从 Ollama 中自动检测第一个聊天模型
            try {
                URL url = URI.create(config.getBaseUrl() + "/api/tags").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);
                String body = readStream(conn.getInputStream());
                conn.disconnect();

                JSONObject resp = JSON.parseObject(body);
                JSONArray models = resp.getJSONArray("models");
                if (models != null && !models.isEmpty()) {
                    for (int i = 0; i < models.size(); i++) {
                        JSONObject m = models.getJSONObject(i);
                        String name = m.getString("name");
                        // 跳过嵌入模型（它们不支持 /api/chat）
                        String lower = name.toLowerCase();
                        if (lower.contains("embed") || lower.contains("nomic") || lower.contains("bert")) {
                            log.debug("Skipping embedding model: {}", name);
                            continue;
                        }
                        detectedChatModel = name;
                        log.info("Ollama chat model auto-detected: {} (configured '{}' not found)",
                                detectedChatModel, configuredModel);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to auto-detect Ollama model: {}", e.getMessage());
            }
            if (detectedChatModel == null) {
                detectedChatModel = configuredModel; // 回退到配置值，让后续调用报错
            }
            return detectedChatModel;
        }
    }

    /**
     * 检测 Ollama 服务是否运行中
     * 调用 /api/tags 接口，返回 HTTP 200 即为可用
     */
    public boolean isRunning() {
        try {
            URL url = URI.create(config.getBaseUrl() + "/api/tags").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检测指定模型是否已拉取到 Ollama（支持模糊匹配，如 "mymodel" 可匹配 "mymodel:latest"）
     * @param modelName 模型名称，如 "qwen2.5:7b"、"mymodel"
     */
    public boolean isModelAvailable(String modelName) {
        try {
            URL url = URI.create(config.getBaseUrl() + "/api/tags").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            String body = readStream(conn.getInputStream());
            conn.disconnect();

            if (body.isEmpty()) return false;
            JSONObject resp = JSON.parseObject(body);
            JSONArray models = resp.getJSONArray("models");
            if (models == null) return false;

            for (int i = 0; i < models.size(); i++) {
                JSONObject m = models.getJSONObject(i);
                String name = m.getString("name");
                // 完全匹配 或 作为前缀匹配（mymodel 可匹配 mymodel:latest）
                if (modelName.equals(name) || name.startsWith(modelName + ":")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Ollama model check failed for '{}': {}", modelName, e.getMessage());
            return false;
        }
    }

    /**
     * 精确查找模型名称（支持模糊匹配，返回 Ollama 中的完整名称如 "mymodel:latest"）
     */
    private String resolveModelName(String modelName) {
        try {
            URL url = URI.create(config.getBaseUrl() + "/api/tags").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            String body = readStream(conn.getInputStream());
            conn.disconnect();

            if (body.isEmpty()) return modelName;
            JSONObject resp = JSON.parseObject(body);
            JSONArray models = resp.getJSONArray("models");
            if (models == null) return modelName;

            for (int i = 0; i < models.size(); i++) {
                JSONObject m = models.getJSONObject(i);
                String name = m.getString("name");
                // 完全匹配或前缀匹配 → 返回 Ollama 中的完整名称
                if (modelName.equals(name) || name.startsWith(modelName + ":")) {
                    return name;
                }
            }
        } catch (Exception e) {
            log.debug("resolveModelName failed for '{}': {}", modelName, e.getMessage());
        }
        return modelName;
    }

    // ==================== Embeddings API ====================

    /**
     * 获取文本的向量嵌入
     */
    public float[] embed(String text) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", config.getEmbeddingModel());
            body.put("prompt", text);

            JSONObject resp = postJson("/api/embeddings", body);
            if (resp == null) return new float[0];

            JSONArray arr = resp.getJSONArray("embedding");
            if (arr == null) return new float[0];

            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = arr.getFloatValue(i);
            }
            return vec;
        } catch (Exception e) {
            // 节流日志：避免每篇文档失败都打印一次
            long now = System.currentTimeMillis();
            if (now - lastEmbedFailLogTime > FAIL_LOG_INTERVAL_MS) {
                lastEmbedFailLogTime = now;
                log.error("Ollama embedding unavailable at {}: {}",
                        config.getBaseUrl(), e.getMessage());
            }
            return new float[0];
        }
    }

    // ==================== HTTP 工具 ====================

    private JSONObject postJson(String path, JSONObject body) throws IOException {
        HttpURLConnection conn = openConnection(path);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
        if (conn.getResponseCode() >= 400) {
            log.warn("Ollama API {} returned: {}", path, readStream(conn.getErrorStream()));
            return null;
        }
        String respBody = readStream(conn.getInputStream());
        return respBody.isEmpty() ? null : JSON.parseObject(respBody);
    }

    private void postJsonStream(String path, JSONObject body, Consumer<String> onLine) throws IOException {
        HttpURLConnection conn = openConnection(path);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onLine.accept(line.trim());
            }
        }
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        URL url = URI.create(config.getBaseUrl() + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(config.getTimeoutSeconds() * 1000);
        conn.setReadTimeout(config.getTimeoutSeconds() * 1000);
        return conn;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
