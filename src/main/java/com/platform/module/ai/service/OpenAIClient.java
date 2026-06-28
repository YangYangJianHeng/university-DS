package com.platform.module.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.platform.module.ai.config.OpenAIConfig;
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
 * OpenAI 兼容 API 客户端
 * 支持 OpenAI、DeepSeek、通义千问等兼容 /v1/ 接口的模型提供商
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIClient {

    private final OpenAIConfig config;

    // ==================== Chat API ====================

    public String chat(List<Map<String, String>> messages) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", config.getChatModel());
            body.put("messages", messages);
            body.put("stream", false);

            JSONObject resp = postJson("/v1/chat/completions", body);
            if (resp == null) return "抱歉，AI 服务暂时不可用，请稍后再试。";

            JSONArray choices = resp.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject msg = choice.getJSONObject("message");
                return msg != null ? msg.getString("content") : "";
            }
            return "";
        } catch (Exception e) {
            log.error("OpenAI chat error: {}", e.getMessage());
            return "抱歉，AI 服务调用失败：" + e.getMessage();
        }
    }

    public void chatStream(List<Map<String, String>> messages,
                           Consumer<String> onChunk, Runnable onDone) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", config.getChatModel());
            body.put("messages", messages);
            body.put("stream", true);

            postStream("/v1/chat/completions", body, line -> {
                if (line == null || line.isEmpty() || "[DONE]".equals(line)) return;
                try {
                    JSONObject chunk = JSON.parseObject(line);
                    JSONArray choices = chunk.getJSONArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                        if (delta != null && delta.containsKey("content")) {
                            String content = delta.getString("content");
                            if (content != null) onChunk.accept(content);
                        }
                    }
                } catch (Exception ignored) {}
            });
            onDone.run();
        } catch (Exception e) {
            log.error("OpenAI stream error: {}", e.getMessage());
            onChunk.accept("抱歉，AI 服务调用失败：" + e.getMessage());
            onDone.run();
        }
    }

    // ==================== Embeddings API ====================

    /**
     * 获取文本向量（OpenAI 兼容格式）
     */
    public float[] embed(String text) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", config.getEmbeddingModel());
            body.put("input", text);

            JSONObject resp = postJson("/v1/embeddings", body);
            if (resp == null) return new float[0];

            JSONArray data = resp.getJSONArray("data");
            if (data == null || data.isEmpty()) return new float[0];

            JSONArray embedding = data.getJSONObject(0).getJSONArray("embedding");
            if (embedding == null) return new float[0];

            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.getFloatValue(i);
            }
            return vec;
        } catch (Exception e) {
            log.error("OpenAI embedding error: {}", e.getMessage());
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
            String errBody = readStream(conn.getErrorStream());
            log.warn("OpenAI API {} returned {}: {}", path, conn.getResponseCode(), errBody);
            return null;
        }
        String respBody = readStream(conn.getInputStream());
        return respBody.isEmpty() ? null : JSON.parseObject(respBody);
    }

    private void postStream(String path, JSONObject body, Consumer<String> onLine) throws IOException {
        HttpURLConnection conn = openConnection(path);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // SSE 格式: "data: {...}"
                if (trimmed.startsWith("data: ")) {
                    onLine.accept(trimmed.substring(6));
                }
            }
        }
    }

    private volatile boolean keyLogged = false;

    /**
     * 直接从 .env 读取 API Key（备用方案，绕过 Spring 配置解析问题）
     */
    private String loadApiKeyFromEnv() {
        // 如果 config 已有值且非空，直接使用
        String configKey = config.getApiKey();
        if (configKey != null && !configKey.isEmpty()) {
            return configKey;
        }

        // 否则直接读取 .env 文件
        try {
            File envFile = findEnvFile();
            if (envFile == null || !envFile.exists()) return configKey;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqIdx = line.indexOf('=');
                    if (eqIdx <= 0) continue;
                    String key = line.substring(0, eqIdx).trim();
                    if ("OPENAI_API_KEY".equals(key)) {
                        String value = line.substring(eqIdx + 1).trim().replaceAll("^\"|\"$", "");
                        log.info("Loaded OPENAI_API_KEY from .env file directly: {}...",
                                value.length() > 8 ? value.substring(0, 8) + "..." : "***");
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read .env for API key: {}", e.getMessage());
        }
        return configKey;
    }

    private File findEnvFile() {
        File f = new File(System.getProperty("user.dir"), ".env");
        if (f.exists()) return f;
        f = new File(".env").getAbsoluteFile();
        return f.exists() ? f : null;
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        String fullUrl = config.getBaseUrl() + path;
        String apiKey = loadApiKeyFromEnv();

        // 首次调用时打印 Key 状态（掩码），方便排查
        if (!keyLogged) {
            keyLogged = true;
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("!!! OPENAI_API_KEY is EMPTY! Check .env file and DotenvEnvironmentPostProcessor.");
                log.error("!!! Ensure .env has OPENAI_API_KEY=sk-xxx and app is restarted after editing.");
            } else {
                String masked = apiKey.length() <= 10 ? "***" : apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
                log.info("OpenAIClient ready: baseUrl={}, apiKey={}, model={}", config.getBaseUrl(), masked, config.getChatModel());
            }
        }

        URL url = URI.create(fullUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
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
