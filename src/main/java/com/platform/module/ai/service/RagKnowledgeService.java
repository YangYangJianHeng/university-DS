package com.platform.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.module.ai.config.RagConfig;
import com.platform.module.ai.service.QdrantVectorStore.DocEntry;
import com.platform.module.competition.entity.Competition;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 知识库服务
 * 使用 Qdrant 向量数据库（配合 Ollama embeddings）进行语义检索
 * 
 * 数据流：
 *   启动 → 从 MySQL 读取项目/竞赛 → Ollama embed → Qdrant upsert
 *   查询 → Ollama embed(question) → Qdrant search → 返回 Top-K 文段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagKnowledgeService {

    private final RagConfig config;
    private final EmbeddingService embeddingService;
    private final QdrantVectorStore vectorStore;
    private final ProjectMapper projectMapper;
    private final CompetitionMapper competitionMapper;

    // 桶名常量
    public static final String BUCKET_PROJECTS = "projects";
    public static final String BUCKET_COMPETITIONS = "competitions";

    /**
     * 应用启动后自动构建知识向量库
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (config.isAutoBuild()) {
            // 延迟执行，确保 Qdrant 完全就绪
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                try {
                    buildAll();
                    log.info("=== RAG knowledge base ready: projects={}, competitions={} (Qdrant={}) ===",
                            vectorStore.size(BUCKET_PROJECTS), 
                            vectorStore.size(BUCKET_COMPETITIONS),
                            vectorStore.isQdrantAvailable() ? "connected" : "fallback");
                } catch (Exception e) {
                    log.error("Failed to build RAG knowledge base: {}", e.getMessage(), e);
                }
            }, "rag-builder").start();
        }
    }

    /**
     * 构建全部知识库
     */
    public synchronized void buildAll() {
        vectorStore.clearAll();
        if (config.isIncludeProjects()) buildProjects();
        if (config.isIncludeCompetitions()) buildCompetitions();
    }

    /**
     * 构建项目知识库（批量 upsert 到 Qdrant）
     */
    public void buildProjects() {
        vectorStore.deleteCollection(BUCKET_PROJECTS);
        vectorStore.ensureCollection(BUCKET_PROJECTS);

        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .ne(Project::getStatus, "CLOSED")
                        .last("LIMIT 500"));

        List<DocEntry> entries = new ArrayList<>();
        for (Project p : projects) {
            String doc = buildProjectDocument(p);
            float[] vec = embeddingService.embed(doc);
            if (vec.length == 0) continue;
            Map<String, String> meta = new HashMap<>();
            meta.put("id", String.valueOf(p.getId()));
            meta.put("name", p.getName() != null ? p.getName() : "");
            meta.put("status", p.getStatus() != null ? p.getStatus() : "");
            entries.add(new DocEntry("project:" + p.getId(), doc, vec, meta));
        }

        vectorStore.addAll(BUCKET_PROJECTS, entries);
        log.info("Built project vector index: {} documents", entries.size());
    }

    /**
     * 构建竞赛知识库
     */
    public void buildCompetitions() {
        vectorStore.deleteCollection(BUCKET_COMPETITIONS);
        vectorStore.ensureCollection(BUCKET_COMPETITIONS);

        List<Competition> competitions = competitionMapper.selectList(
                new LambdaQueryWrapper<Competition>()
                        .last("LIMIT 500"));

        List<DocEntry> entries = new ArrayList<>();
        for (Competition c : competitions) {
            String doc = buildCompetitionDocument(c);
            float[] vec = embeddingService.embed(doc);
            if (vec.length == 0) continue;
            Map<String, String> meta = new HashMap<>();
            meta.put("id", String.valueOf(c.getId()));
            meta.put("name", c.getName() != null ? c.getName() : "");
            meta.put("category", c.getCategory() != null ? c.getCategory() : "");
            meta.put("deadline", c.getRegisterEnd() != null ? c.getRegisterEnd().toString() : "");
            entries.add(new DocEntry("competition:" + c.getId(), doc, vec, meta));
        }

        vectorStore.addAll(BUCKET_COMPETITIONS, entries);
        log.info("Built competition vector index: {} documents", entries.size());
    }

    /**
     * 语义搜索：根据用户问题检索相关知识
     */
    public String searchContext(String question, int topK) {
        float[] queryVec = embeddingService.embed(question);
        if (queryVec.length == 0) {
            log.warn("Embedding returned empty vector for question: {}", 
                    question.length() > 50 ? question.substring(0, 50) + "..." : question);
            return "";
        }

        StringBuilder ctx = new StringBuilder();
        List<DocEntry> results = vectorStore.searchAll(queryVec, topK);
        for (DocEntry doc : results) {
            ctx.append("---\n").append(doc.content()).append("\n");
        }

        if (results.isEmpty()) {
            log.debug("No RAG results found for question");
        } else {
            log.debug("RAG search returned {} results", results.size());
        }

        return ctx.toString();
    }

    // ==================== 文档构建 ====================

    private String buildProjectDocument(Project p) {
        return String.format(
                "项目名称：%s\n描述：%s\n状态：%s\n",
                p.getName() != null ? p.getName() : "",
                p.getDescription() != null ? p.getDescription() : "",
                p.getStatus() != null ? p.getStatus() : ""
        );
    }

    private String buildCompetitionDocument(Competition c) {
        return String.format(
                "竞赛名称：%s\n类别：%s\n描述：%s\n级别：%s\n报名截止：%s\n赛程安排：%s\n",
                c.getName() != null ? c.getName() : "",
                c.getCategory() != null ? c.getCategory() : "",
                c.getDescription() != null ? c.getDescription() : "",
                c.getLevel() != null ? c.getLevel() : "",
                c.getRegisterEnd() != null ? c.getRegisterEnd().toString() : "",
                c.getSchedule() != null ? c.getSchedule() : ""
        );
    }
}
