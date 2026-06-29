package com.platform.module.ai.controller;

import com.platform.common.result.Result;
import com.platform.module.ai.service.QdrantVectorStore;
import com.platform.module.ai.service.RagKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 知识库管理控制器
 * 提供知识库重建、状态查询等管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagKnowledgeController {

    private final RagKnowledgeService ragService;
    private final QdrantVectorStore vectorStore;

    // ==================== 知识库重建 ====================

    /**
     * 手动重建全部知识库（项目 + 竞赛）
     */
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild() {
        log.info("Manual RAG knowledge base rebuild triggered");
        try {
            ragService.buildAll();
            Map<String, Object> result = new HashMap<>();
            result.put("projects", vectorStore.size(QdrantVectorStore.COLLECTION_PROJECTS));
            result.put("competitions", vectorStore.size(QdrantVectorStore.COLLECTION_COMPETITIONS));
            result.put("qdrantAvailable", vectorStore.isQdrantAvailable());
            log.info("RAG rebuild completed: projects={}, competitions={}",
                    result.get("projects"), result.get("competitions"));
            return Result.success(result);
        } catch (Exception e) {
            log.error("RAG rebuild failed", e);
            return Result.error("重建知识库失败: " + e.getMessage());
        }
    }

    /**
     * 重建项目知识库
     */
    @PostMapping("/rebuild/projects")
    public Result<Map<String, Object>> rebuildProjects() {
        log.info("Manual project knowledge base rebuild triggered");
        try {
            ragService.buildProjects();
            Map<String, Object> result = new HashMap<>();
            result.put("projects", vectorStore.size(QdrantVectorStore.COLLECTION_PROJECTS));
            result.put("qdrantAvailable", vectorStore.isQdrantAvailable());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Project RAG rebuild failed", e);
            return Result.error("重建项目知识库失败: " + e.getMessage());
        }
    }

    /**
     * 重建竞赛知识库
     */
    @PostMapping("/rebuild/competitions")
    public Result<Map<String, Object>> rebuildCompetitions() {
        log.info("Manual competition knowledge base rebuild triggered");
        try {
            ragService.buildCompetitions();
            Map<String, Object> result = new HashMap<>();
            result.put("competitions", vectorStore.size(QdrantVectorStore.COLLECTION_COMPETITIONS));
            result.put("qdrantAvailable", vectorStore.isQdrantAvailable());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Competition RAG rebuild failed", e);
            return Result.error("重建竞赛知识库失败: " + e.getMessage());
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 查询 RAG 知识库和 Qdrant 状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("qdrantAvailable", vectorStore.isQdrantAvailable());
        result.put("projects_count", vectorStore.size(QdrantVectorStore.COLLECTION_PROJECTS));
        result.put("competitions_count", vectorStore.size(QdrantVectorStore.COLLECTION_COMPETITIONS));
        return Result.success(result);
    }

    /**
     * 清空全部知识库
     */
    @DeleteMapping("/clear")
    public Result<String> clearAll() {
        log.info("Clearing all RAG knowledge bases");
        vectorStore.clearAll();
        return Result.success("已清空全部知识库");
    }
}
