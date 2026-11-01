# university-DS
# 青创协同 · 项目总结与AI智能体功能介绍

---

## 一、项目概览

**项目名称**：青创协同 — 大学生竞赛/科研项目组队匹配与过程管理平台

**技术栈**：

| 层次 | 技术选型 | 版本 |
|------|----------|------|
| 后端框架 | Spring Boot | 3.2.0 |
| 持久层 | MyBatis-Plus + MySQL 8 | 3.5.5 |
| 缓存 | Redis + Lettuce 连接池 | — |
| 安全认证 | Spring Security + JWT (jjwt) | 0.12.3 |
| API 文档 | Knife4j (Swagger) | 4.4.0 |
| 实时通信 | WebSocket (Spring) | — |
| 前端 | Vue 3 + Element Plus + TypeScript | — |
| 👇 AI 智能体 | 见下一节 | — |

**核心业务模块**：竞赛中心、项目管理、招募广场、任务中心、我的团队、成果展厅、AI智能助手

**数据库规模**：27张业务表（含AI聊天、徽章、证书等），统一由 `init-all.sql` 管理

---

## 二、AI 智能体架构总览

本项目基于 **LangChain4j 0.36.2** 构建了一套简易智能体（Agent），实现了从意图识别、上下文收集、RAG检索到回复生成的完整闭环。整体架构如下：

```
┌──────────────────────────────────────────────────────────────┐
│                      前端 (Vue3)                              │
│          SSE 流式接收  |  会话管理  |  预设问题卡片            │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP POST / SSE
┌──────────────────────▼───────────────────────────────────────┐
│                   AIChatController                            │
│         会话 CRUD  |  同步/流式发送消息                        │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                    AIChatService                               │
│         创建会话  |  消息收发  |  权限校验                      │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│              ★ AiAgentService (简易Agent) ★                   │
│                                                               │
│  ① classifyIntent   → 关键词规则 + 意图分类                    │
│  ② collectUserContext → MySQL 查询用户项目/任务/竞赛数据       │
│  ③ RAG检索           → Qdrant 语义搜索                        │
│  ④ checkAndCompress  → 对话历史智能压缩                       │
│  ⑤ buildPrompt       → 组装 System/上下文/历史/当前消息        │
│  ⑥ doChat/doChatStream → 调用 LLM (LangChain4j / HTTP API)   │
│  ⑦ saveMessage       → 持久化到 MySQL                         │
│  ⑧ autoTitleSession  → 自动生成会话标题                       │
└──────────────────────┬───────────────────────────────────────┘
                       │
          ┌────────────┼──────────────┐
          ▼            ▼              ▼
   ┌──────────┐ ┌──────────┐ ┌──────────────┐
   │ Ollama   │ │ DeepSeek │ │ Qdrant 向量库 │
   │ (本地)   │ │ (云端)   │ │ (Docker)     │
   │ 聊天模型 │ │ 聊天模型 │ │ 语义检索     │
   │ 嵌入模型 │ │ (备用)   │ │              │
   └──────────┘ └──────────┘ └──────────────┘
```

---

## 三、LangChain4j 简易 Agent 智能体

### 3.1 Agent 工作流（模拟 LangGraph 节点式架构）

`AiAgentService` 是整个 AI 功能的核心大脑，实现了 **8步流水线**：

```
用户消息 → [①意图分类] → [②上下文收集] → [③RAG检索] 
       → [④历史压缩] → [⑤Prompt构建] → [⑥LLM调用] 
       → [⑦消息持久化] → [⑧标题生成]
```

### 3.2 意图分类 — 关键词规则引擎

```java
// AiAgentService.classifyIntent()
// 基于关键词匹配将用户意图分为4类

PROJECT    → "项目" "任务" "进度" "里程碑" "逾期" "甘特图"
COMPETITION → "比赛" "竞赛" "报名" "备赛" "答辩" "选题"
TEAM       → "团队" "队员" "导师" "招人" "招募" "组队"
GENERAL    → 以上都不匹配（闲聊、问知识、写代码等）
```

**设计考量**：意图影响后续全链路行为——GENERAL 意图会**跳过 RAG 检索和平台数据注入**，避免不相关数据污染对话质量。

### 3.3 上下文收集 — 按需加载

根据用户意图，从 MySQL 中查询 4 类上下文数据：

| 意图 | 收集的数据 | 数据量控制 |
|------|-----------|-----------|
| GENERAL | 仅用户基本信息（昵称、专业、年级） | 1 次查询 |
| PROJECT | 用户基本信息 + 所属团队的5个最新项目 + 项目关联的20个任务 | 4 次查询 |
| COMPETITION | 项目数据 + 按专业匹配的15个竞赛（不足3个降级全量查询） | 5 次查询 |
| TEAM | 用户基本信息 + 团队数据 | 3 次查询 |

### 3.4 Prompt 构建策略

每一次对话的系统提示词由 **4 层组装**：

```
┌─ System Prompt ────────────────────────────────┐
│ 角色定义："小青"学长/学姐，温暖亲切，中文回复      │
│ 通用场景：自由对话，无需关联平台数据               │
│ 专业场景：结合用户背景推荐竞赛、分析进度、不编造    │
└────────────────────────────────────────────────┘
┌─ 实时上下文 ─────────────────────────────────────┐
│ 当前时间：2026年6月29日 星期一 21:21               │
│ 当前天气：北京, 晴, 28°C, 湿度45%                 │
│ (通过 wttr.in API + 10分钟缓存)                    │
└────────────────────────────────────────────────┘
┌─ 意图引导 ───────────────────────────────────────┐
│ COMPETITION: "请基于用户专业推荐3-5个竞赛..."      │
│ PROJECT: "请分析项目健康度，逾期任务重点提醒..."    │
│ TEAM: "请基于团队数据给出协作优化建议..."           │
│ GENERAL: "自由对话，无需关联平台数据..."            │
└────────────────────────────────────────────────┘
┌─ RAG 知识 + 用户数据 ─────────────────────────────┐
│ 知识库检索结果（Qdrant Top-3 语义匹配）            │
│ 用户的项目列表、任务统计、竞赛推荐列表              │
└────────────────────────────────────────────────┘
```

### 3.5 对话压缩 — 自动摘要

当单次会话消息数超过阈值（默认15轮=30条），`ChatCompressionService` 自动触发压缩：

1. 保留最近 5 轮完整消息
2. 将早期消息发送给 LLM 生成 200 字摘要
3. 摘要作为 system 消息注入，旧消息在 DB 中标记 `compressed=1`
4. 后续查询仅取 `compressed=0` 的消息，保持性能

### 3.6 实时上下文增强

`ContextEnrichmentService` 解决了**本地 Ollama 模型不知道"现在几点了""今天天气怎样"**的问题：

- **时间注入**：`Asia/Shanghai` 时区精确到秒，含星期、是否周末
- **天气注入**：调用免费 `wttr.in` API，10分钟缓存，含温度/湿度/风速
- **原理**：在 System Prompt 中直接注入真实数据 → 任何模型都能用，无需 function calling

---

## 四、RAG 向量数据库与知识库

### 4.1 整体架构

```
┌──────────────────────────────────────────────────────────┐
│                    应用启动时（自动）                       │
│                                                          │
│  MySQL ──→ 读取项目列表 ──→ Ollama Embedding ──→ Qdrant  │
│       └──→ 读取竞赛列表 ──→ nomic-embed-text ──→ upsert  │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                    用户对话时                              │
│                                                          │
│  用户问题 ──→ Ollama Embedding ──→ Qdrant 搜索(Top-K)    │
│       └──→ 返回语义相关文段 ──→ 注入 Prompt ──→ LLM 回答  │
└──────────────────────────────────────────────────────────┘
```

### 4.2 环境配置与部署

**Qdrant 向量数据库** — Docker Compose 一键部署：

```yaml
# docker-compose-qdrant.yml
services:
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"   # REST API
      - "6334:6334"   # gRPC
    volumes:
      - ./qdrant_data:/qdrant/storage  # 数据持久化到项目目录
```

**管理面板**：`http://localhost:6333/dashboard` | **API文档**：`http://localhost:6333/docs`

### 4.3 嵌入模型配置

| 提供商 | 模型 | 向量维度 | 用途 |
|--------|------|----------|------|
| Ollama (本地) | `nomic-embed-text` | 768 | 首选，免费、离线可用 |
| OpenAI API | `text-embedding-3-small` | 1536 | 云端备用 |
| DeepSeek | — | — | **不支持 embedding，自动跳过** |

**核心路由逻辑** (`EmbeddingService`)：
```
embed() 调用:
  ├── 首选 Ollama embed → nomic-embed-text
  ├── 失败 → OpenAI embed (DeepSeek 自动跳过)
  └── 都失败 → 返回空向量 + 60秒节流日志
```

### 4.4 向量存储实现

`QdrantVectorStore` 实现了**双模存储策略**：

| 模式 | 存储引擎 | 相似度算法 | 故障降级 |
|------|----------|-----------|---------|
| 生产模式 | Qdrant REST API | Cosine | — |
| 回退模式 | 内存 ConcurrentHashMap | Cosine (手动) | Qdrant 不可用时自动切换 |

**核心 API**：
- `ensureCollection(name)` — 自动创建 768 维 Cosine 集合
- `addAll(bucket, entries)` — 批量 upsert 文档向量
- `searchAll(queryVec, topK)` — 跨 projects + competitions 桶联合检索
- `isQdrantAvailable()` — 实时健康检查

### 4.5 知识库内容覆盖

| 知识桶 | 数据来源 | 文档格式 | 自动构建 |
|--------|----------|----------|----------|
| `projects` | MySQL project 表（排除 CLOSED，限制500条） | 名称+描述+状态 | 启动后 3 秒 |
| `competitions` | MySQL competition 表（限制500条） | 名称+类别+描述+级别+报名截止+赛程 | 启动后 3 秒 |

**配置开关** (`.env`)：
```bash
RAG_AUTO_BUILD=true           # 启动时自动构建向量库
RAG_INCLUDE_PROJECTS=true     # 包含项目知识
RAG_INCLUDE_COMPETITIONS=true # 包含竞赛知识
RAG_INCLUDE_TEAMS=false       # 暂不包含团队数据
```

---

## 五、模型提供商自动检测与回退

### 5.1 LangChain4jConfig 启动策略

```
应用启动
  ├── 检测 Ollama localhost:11434/api/tags
  │     ├── 成功 → 使用 OllamaChatModel + OllamaEmbeddingModel
  │     └── 失败 → 回退到 DeepSeek API
  └── 聊天模型: Ollama > DeepSeek（自动回退）
      嵌入模型: Ollama > OpenAI embedding（DeepSeek 自动跳过）
```

### 5.2 流式输出策略

```java
// AiAgentService.doChatStream()
if (ollamaClient.isRunning()) {
    ollamaClient.chatStream(model, messages, onChunk, onDone); // ★ 本地流式
} else {
    openAIClient.chatStream(messages, onChunk, onDone);        // ☆ 云端流式
}
```

### 5.3 .env 配置一览

```bash
# 本地 Ollama（聊天 + 嵌入）
OLLAMA_CHAT_MODEL=mymodel               # 自动检测模型名
OLLAMA_EMBEDDING_MODEL=nomic-embed-text # 嵌入模型（768维）
OLLAMA_BASE_URL=http://localhost:11434   # Ollama 服务地址

# 云端 DeepSeek（仅聊天，不支持 embedding）
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_API_KEY=sk-xxx
OPENAI_CHAT_MODEL=deepseek-v4-flash

# Qdrant 向量数据库
RAG_STORE_TYPE=qdrant_local
QDRANT_BASE_URL=http://localhost:6333
QDRANT_VECTOR_SIZE=768
QDRANT_ENABLED=true
```

---

## 六、Skill 文档驱动的智能问答

### 6.1 Skill 体系架构

项目维护了 **8 个 Skill 文档**，形成完整的领域知识体系：

```
skills/
├── README.md                     # Skill 索引与使用指南
├── project-overview/SKILL.md     # 项目全貌、技术栈导航
├── backend-development/SKILL.md  # Spring Boot 后端开发规范
├── frontend-development/SKILL.md # Vue3 前端开发规范
├── database-constraints/SKILL.md # 数据库设计与约束
├── ai-integration/SKILL.md       # AI 集成架构（LangChain4j/Qdrant）
├── ai-chat-assistant/SKILL.md    # AI 助手"小星"15+场景回答策略 ★
├── prompt-understanding/SKILL.md # 提示词理解与需求澄清
```

### 6.2 Skill 与 AI 代码生成的联动

Skill 文件通过 `description` 字段声明触发条件，AI 编码助手根据任务上下文**自动加载**对应 Skill：

```yaml
# skills/ai-chat-assistant/SKILL.md 头部
---
name: ai-chat-assistant
description: >-
  覆盖 15+ 对话场景的回答策略、意图识别、情感感知、主动建议生成...
  Use when designing AI responses, building conversation flows...
---
```

当用户提出以下需求时，AI 助手自动加载该 Skill：
- "优化欢迎语" → 加载 `ai-chat-assistant` 的场景1策略
- "推荐竞赛" → 加载竞赛智能推荐场景（匹配度算法+回答模板）
- "项目风险分析" → 加载风险评估场景（4维权重+等级判定）

### 6.3 15+ 场景覆盖

`ai-chat-assistant/SKILL.md` 定义了完整的回答策略矩阵：

| # | 场景 | 核心策略 |
|---|------|----------|
| 1 | 欢迎与自我介绍 | 3-5项能力介绍 + 快捷入口卡片 |
| 2 | 项目进度总览 | 总→分结构，进度可视化，风险项目红色标注 |
| 3 | 任务详情与提醒 | 逾期>临期>正常排序，具体行动建议 |
| 4 | 竞赛智能推荐 | 技能匹配×0.4 + 专业相关×0.25 + 偏好×0.2 + 时间紧迫×0.15 |
| 5 | 团队效率分析 | 负载均衡度 + 完成速度 + 活跃度 三维诊断 |
| 6 | 智能导师推荐 | 研究方向匹配 + 负载 + 认证 + 评分 |
| 7 | 项目风险评估 | 逾期率40%+活跃度25%+里程碑20%+规模15% |
| 14 | 多轮对话 | 关键实体追踪 + 代词消解 |
| 15 | 复杂问题拆解 | 子问题识别 + 序号标注 + 逐个回答 |

### 6.4 语气温度调节机制

Skill 定义了**情感感知的温度调节**，让 AI 回答更有人情味：

| 用户情绪 | 回复温度 | emoji 用量 | 语气策略 |
|----------|----------|-----------|----------|
| 积极兴奋 | 热 (85%) | 2-3 个 | 活泼鼓励 |
| 正常中性 | 温 (60%) | 1-2 个 | 专业清晰 |
| 焦虑沮丧 | 暖 (70%) | 1 个 | 先共情再给方案 → ★ |
| 愤怒不满 | 冷 (40%) | 0 个 | 理性克制 |

---

## 七、AI 功能亮点总结

### 7.1 "避重就轻"设计哲学

| 原则 | 实现方式 |
|------|----------|
| **不编造数据** | System Prompt 明确约束"绝不编造不存在的数据"，数据为空时引导用户前往功能模块 |
| **GENERAL 意图隔离** | 闲聊时跳过 RAG 和平台数据注入，避免不相关数据污染回答 |
| **失败优雅降级** | Ollama 不可用→自动回退 DeepSeek；Qdrant 不可用→内存余弦相似度；嵌入失败→空向量 |
| **日志控制** | embed 错误 60 秒节流，避免启动时数千条日志刷屏 |
| **安全红线** | 绝不透露他人数据、不评价具体师生、不提供作弊建议 |

### 7.2 技术特色

1. **轻量级 Agent 设计**：不依赖 LangGraph 等重框架，用纯 Java 方法链实现 8 步节点式工作流，清晰可调试
2. **自研 HTTP 客户端**：`OllamaClient` 和 `OpenAIClient` 直接调用 REST API，零中间依赖，支持流式 SSE 解析
3. **自动模型检测**：启动时探测 Ollama 模型列表，自动跳过 nomic-embed-text 等嵌入模型，智能选择第一个可用聊天模型
4. **上下文注入**：通过 system prompt 注入真实时间/天气，让任何模型都能回答实时问题，无需 function calling
5. **对话摘要压缩**：长对话自动触发 LLM 摘要，保留关键信息的同时控制 token 消耗

---

## 八、快速启动指南

```bash
# 1. 启动 Qdrant 向量数据库
docker compose -f docker-compose-qdrant.yml up -d

# 2. 启动 Ollama（可选，离线/隐私场景）
ollama serve
ollama pull mymodel            # 聊天模型
ollama pull nomic-embed-text   # 嵌入模型

# 3. 配置 .env
OPENAI_API_KEY=sk-xxx          # DeepSeek API Key（云端备用）

# 4. 启动应用
mvn spring-boot:run

# 5. 验证
# - Qdrant 面板：http://localhost:6333/dashboard
# - Swagger API：http://localhost:8080/doc.html
```

**应用启动日志预期**：
```
[LangChain4j] Loaded X variables from .env
✅ Ollama detected: baseUrl=http://localhost:11434, model=mymodel
🟢 Auto-detected Ollama → using OllamaChatModel
🟢 Auto-detected Ollama → using OllamaEmbeddingModel (nomic-embed-text)
Qdrant connected at http://localhost:6333 ✓
=== RAG knowledge base ready: projects=42, competitions=56 (Qdrant=connected) ===
```

---

> **文档生成时间**：2026年6月29日  
> **项目版本**：v1.0.0 · Spring Boot 3.2 · LangChain4j 0.36.2  
> **技术博客风格**：避重就轻，突出架构设计亮点与工程实践
