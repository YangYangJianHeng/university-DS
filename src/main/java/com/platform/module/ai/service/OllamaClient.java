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
     */
    public void chatStream(String model, List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onDone) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
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
