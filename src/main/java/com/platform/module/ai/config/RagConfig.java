package com.platform.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "platform.ai.rag")
public class RagConfig {
    /** 向量存储类型: qdrant_local / qdrant_remote / inmemory */
    private String storeType = "qdrant_local";
    /** Qdrant REST API 地址 */
    private String qdrantBaseUrl = "http://localhost:6333";
    /** Qdrant 集合名称前缀 */
    private String qdrantCollectionPrefix = "university_ds";
    /** 向量维度（nomic-embed-text = 768） */
    private int qdrantVectorSize = 768;
    /** Qdrant 数据持久化路径 */
    private String qdrantStoragePath = "./qdrant_data";
    /** 是否启用 Qdrant */
    private boolean qdrantEnabled = true;
    /** 是否启动时自动构建知识库 */
    private boolean autoBuild = true;
    /** 知识库是否包含项目信息 */
    private boolean includeProjects = true;
    /** 知识库是否包含竞赛信息 */
    private boolean includeCompetitions = true;
    /** 知识库是否包含团队信息 */
    private boolean includeTeams = false;
}
