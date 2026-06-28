package com.platform.module.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.platform.module.ai.config.RagConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Qdrant 向量存储服务
 * 生产级单机向量数据库，支持文件持久化
 * 
 * 数据流：
 *   Ollama embed(text) → float[] → Qdrant REST API (upsert)
 *   用户提问 → Ollama embed(question) → Qdrant search → 返回 Top-K 文档
 * 
 * 支持两种模式：
 *   1. qdrant_local / qdrant_remote: 通过 REST API 连接 Qdrant 服务
 *   2. 当 Qdrant 不可用时自动回退到内存存储（保持可用性）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantVectorStore {

    private final RagConfig config;

    // ============ 集合名称常量 ============
    public static final String COLLECTION_PROJECTS = "projects";
    public static final String COLLECTION_COMPETITIONS = "competitions";

    // ============ 内存回退存储（Qdrant 不可用时使用） ============
    private final Map<String, List<DocEntry>> fallbackStore = new ConcurrentHashMap<>();
    private boolean qdrantAvailable = false;

    /** 文档条目 */
    public record DocEntry(String id, String content, float[] vector, Map<String, String> metadata) {}

    @PostConstruct
    public void init() {
        if (config.isQdrantEnabled()) {
            qdrantAvailable = checkQdrantHealth();
            if (qdrantAvailable) {
                log.info("Qdrant connected at {} ✓", config.getQdrantBaseUrl());
            } else {
                log.warn("Qdrant not available at {}, will use in-memory fallback", config.getQdrantBaseUrl());
            }
        }
    }

    // ==================== 健康检查 ====================

    private boolean checkQdrantHealth() {
        try {
            String url = config.getQdrantBaseUrl() + "/collections";
            HttpURLConnection conn = openConnection(url, "GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 集合管理 ====================

    /**
     * 确保集合存在（不存在则创建）
     */
    public void ensureCollection(String name) {
        String fullName = fullCollectionName(name);
        if (qdrantAvailable) {
            try {
                // 检查是否存在
                String url = config.getQdrantBaseUrl() + "/collections/" + fullName;
                HttpURLConnection conn = openConnection(url, "GET");
                int code = conn.getResponseCode();
                conn.disconnect();

                if (code == 200) {
                    log.debug("Collection '{}' already exists", fullName);
                    return;
                }
                // 创建集合
                createQdrantCollection(fullName);
            } catch (Exception e) {
                log.error("Failed to ensure collection '{}': {}", fullName, e.getMessage());
            }
        }
    }

    private void createQdrantCollection(String fullName) throws IOException {
        JSONObject body = new JSONObject();
        JSONObject vectors = new JSONObject();
        vectors.put("size", config.getQdrantVectorSize());
        vectors.put("distance", "Cosine");
        body.put("vectors", vectors);

        String url = config.getQdrantBaseUrl() + "/collections/" + fullName;
        HttpURLConnection conn = openConnection(url, "PUT");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        conn.disconnect();

        if (code == 200 || code == 201) {
            log.info("Qdrant collection '{}' created (dim={})", fullName, config.getQdrantVectorSize());
        } else {
            log.warn("Failed to create collection '{}', code={}", fullName, code);
        }
    }

    /**
     * 删除集合
     */
    public void deleteCollection(String name) {
        String fullName = fullCollectionName(name);
        if (qdrantAvailable) {
            try {
                String url = config.getQdrantBaseUrl() + "/collections/" + fullName;
                HttpURLConnection conn = openConnection(url, "DELETE");
                conn.getResponseCode();
                conn.disconnect();
                log.info("Qdrant collection '{}' deleted", fullName);
            } catch (Exception e) {
                log.error("Failed to delete collection '{}': {}", fullName, e.getMessage());
            }
        }
        // 同时清理回退存储
        fallbackStore.remove(name);
    }

    // ==================== 文档操作 ====================

    /**
     * 批量添加文档（upsert points 到 Qdrant）
     */
    public void addAll(String bucket, List<DocEntry> entries) {
        if (entries.isEmpty()) return;

        String fullName = fullCollectionName(bucket);

        if (qdrantAvailable) {
            ensureCollection(bucket);
            try {
                addPointsToQdrant(fullName, entries);
            } catch (Exception e) {
                log.error("Qdrant upsert failed, falling back to memory: {}", e.getMessage());
                addToFallback(bucket, entries);
            }
        } else {
            addToFallback(bucket, entries);
        }
    }

    /**
     * 添加单个文档
     */
    public void add(String bucket, String id, String content, float[] vector, Map<String, String> metadata) {
        if (qdrantAvailable) {
            ensureCollection(bucket);
            try {
                addPointToQdrant(fullCollectionName(bucket), id, vector, metadata);
            } catch (Exception e) {
                addToFallback(bucket, id, content, vector, metadata);
            }
        } else {
            addToFallback(bucket, id, content, vector, metadata);
        }
    }

    /**
     * 语义搜索（在指定桶中）
     */
    public List<DocEntry> search(String bucket, float[] queryVec, int topK) {
        if (qdrantAvailable) {
            try {
                return searchQdrant(fullCollectionName(bucket), queryVec, topK);
            } catch (Exception e) {
                log.warn("Qdrant search failed, using fallback: {}", e.getMessage());
            }
        }
        return searchFallback(bucket, queryVec, topK);
    }

    /**
     * 跨所有桶搜索
     */
    public List<DocEntry> searchAll(float[] queryVec, int topK) {
        List<DocEntry> results = new ArrayList<>();

        if (qdrantAvailable) {
            for (String bucket : List.of(COLLECTION_PROJECTS, COLLECTION_COMPETITIONS)) {
                try {
                    results.addAll(searchQdrant(fullCollectionName(bucket), queryVec, topK));
                } catch (Exception e) {
                    log.warn("Qdrant search '{}' failed: {}", bucket, e.getMessage());
                }
            }
        }

        // 跨桶去重
        Set<String> seen = new HashSet<>();
        List<DocEntry> unique = new ArrayList<>();
        for (DocEntry e : results) {
            if (seen.add(e.id())) unique.add(e);
        }

        // 如果 Qdrant 不可用或结果为空，尝试回退存储
        if (unique.isEmpty()) {
            for (String bucket : fallbackStore.keySet()) {
                unique.addAll(searchFallback(bucket, queryVec, topK));
            }
        }

        return unique.size() > topK ? unique.subList(0, topK) : unique;
    }

    // ==================== Qdrant REST API 操作 ====================

    private void addPointsToQdrant(String collectionName, List<DocEntry> entries) throws IOException {
        JSONObject body = new JSONObject();
        JSONArray points = new JSONArray();

        for (int i = 0; i < entries.size(); i++) {
            DocEntry e = entries.get(i);
            JSONObject point = new JSONObject();
            // 使用 entry id 的哈希作为 Qdrant point id
            point.put("id", Math.abs(e.id().hashCode()));
            point.put("vector", toJSONArray(e.vector()));

            JSONObject payload = new JSONObject();
            payload.put("doc_id", e.id());
            payload.put("content", e.content());
            if (e.metadata() != null) {
                e.metadata().forEach(payload::put);
            }
            point.put("payload", payload);
            points.add(point);
        }

        body.put("points", points);

        String url = config.getQdrantBaseUrl() + "/collections/" + collectionName + "/points?wait=true";
        executeRequest(url, "PUT", body);
    }

    private void addPointToQdrant(String collectionName, String id, float[] vector, 
                                   Map<String, String> metadata) throws IOException {
        JSONObject body = new JSONObject();
        JSONArray points = new JSONArray();
        JSONObject point = new JSONObject();
        point.put("id", Math.abs(id.hashCode()));
        point.put("vector", toJSONArray(vector));

        JSONObject payload = new JSONObject();
        payload.put("doc_id", id);
        if (metadata != null) {
            metadata.forEach(payload::put);
        }
        point.put("payload", payload);
        points.add(point);
        body.put("points", points);

        String url = config.getQdrantBaseUrl() + "/collections/" + collectionName + "/points?wait=true";
        executeRequest(url, "PUT", body);
    }

    private List<DocEntry> searchQdrant(String collectionName, float[] queryVec, int topK) throws IOException {
        JSONObject body = new JSONObject();
        body.put("vector", toJSONArray(queryVec));
        body.put("limit", topK);
        body.put("with_payload", true);

        String url = config.getQdrantBaseUrl() + "/collections/" + collectionName + "/points/search";
        JSONObject resp = executeRequest(url, "POST", body);

        if (resp == null) return Collections.emptyList();

        List<DocEntry> results = new ArrayList<>();
        JSONArray points = resp.getJSONArray("result");
        if (points == null) return results;

        for (int i = 0; i < points.size(); i++) {
            JSONObject hit = points.getJSONObject(i);
            JSONObject payload = hit.getJSONObject("payload");
            if (payload == null) continue;

            String docId = payload.getString("doc_id");
            String content = payload.getString("content");
            Map<String, String> meta = new HashMap<>();
            for (String key : payload.keySet()) {
                if (!"doc_id".equals(key) && !"content".equals(key)) {
                    meta.put(key, payload.getString(key));
                }
            }
            results.add(new DocEntry(docId != null ? docId : "", 
                    content != null ? content : "", null, meta));
        }
        return results;
    }

    // ==================== 清理操作 ====================

    public void clear(String bucket) {
        if (qdrantAvailable) {
            try {
                deleteCollection(bucket);
                ensureCollection(bucket);
            } catch (Exception e) {
                log.warn("Failed to clear Qdrant collection '{}': {}", bucket, e.getMessage());
            }
        }
        fallbackStore.remove(bucket);
    }

    public void clearAll() {
        for (String bucket : List.of(COLLECTION_PROJECTS, COLLECTION_COMPETITIONS)) {
            clear(bucket);
        }
    }

    public int size(String bucket) {
        if (qdrantAvailable) {
            try {
                String url = config.getQdrantBaseUrl() + "/collections/" + fullCollectionName(bucket);
                HttpURLConnection conn = openConnection(url, "GET");
                String respBody = readResponse(conn);
                conn.disconnect();
                if (respBody != null) {
                    JSONObject resp = JSON.parseObject(respBody);
                    JSONObject result = resp.getJSONObject("result");
                    if (result != null && result.containsKey("points_count")) {
                        return result.getInteger("points_count");
                    }
                }
            } catch (Exception ignored) {}
        }
        return fallbackStore.getOrDefault(bucket, Collections.emptyList()).size();
    }

    // ==================== 内存回退存储 ====================

    private void addToFallback(String bucket, List<DocEntry> entries) {
        fallbackStore.computeIfAbsent(bucket, k -> new CopyOnWriteArrayList<>()).addAll(entries);
    }

    private void addToFallback(String bucket, String id, String content, float[] vector, 
                                Map<String, String> metadata) {
        fallbackStore.computeIfAbsent(bucket, k -> new CopyOnWriteArrayList<>())
                .add(new DocEntry(id, content, vector, metadata));
    }

    private List<DocEntry> searchFallback(String bucket, float[] queryVec, int topK) {
        List<DocEntry> docs = fallbackStore.getOrDefault(bucket, Collections.emptyList());
        if (docs.isEmpty() || queryVec == null || queryVec.length == 0) return Collections.emptyList();

        record ScoredDoc(DocEntry doc, double score) {}
        List<ScoredDoc> scored = new ArrayList<>();
        for (DocEntry doc : docs) {
            double sim = cosineSimilarity(queryVec, doc.vector());
            if (sim > 0) scored.add(new ScoredDoc(doc, sim));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));

        List<DocEntry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            result.add(scored.get(i).doc());
        }
        return result;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ==================== HTTP 工具 ====================

    private JSONObject executeRequest(String urlStr, String method, JSONObject body) throws IOException {
        HttpURLConnection conn = openConnection(urlStr, method);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
        String respBody = readResponse(conn);
        conn.disconnect();
        return respBody != null ? JSON.parseObject(respBody) : null;
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400 
                ? conn.getInputStream() 
                : conn.getErrorStream();
        if (is == null) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private String fullCollectionName(String bucket) {
        return config.getQdrantCollectionPrefix() + "_" + bucket;
    }

    private JSONArray toJSONArray(float[] vec) {
        JSONArray arr = new JSONArray();
        for (float v : vec) arr.add(v);
        return arr;
    }

    public boolean isQdrantAvailable() {
        return qdrantAvailable;
    }
}
