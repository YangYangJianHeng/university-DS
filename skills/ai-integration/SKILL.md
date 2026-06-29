---
name: ai-integration
description: >-
  「组队星」AI 智能集成规范。基于 LangChain4j 0.36.2 框架，集成 DeepSeek Chat API（OpenAI 兼容协议）
  和 Ollama 本地嵌入模型，使用 Qdrant 向量数据库做 RAG 语义检索，通过 SSE 流式输出对话。
  Use when building AI chat, semantic search, intelligent recommendation, RAG pipeline,
  or any AI-related feature in the university platform.
---

# AI 智能集成规范

## 架构概览

```
用户消息 → Controller → AIChatService
                              ├── OpenAiChatModel (DeepSeek API, 流式 SSE)
                              ├── OllamaEmbeddingModel (本地, bge-m3)
                              └── QdrantVectorStore (向量相似度检索)
                                        ↕
                              知识库（竞赛信息、项目文档、使用帮助）
```

## 技术选型

| 组件 | 版本 | 用途 |
|------|------|------|
| LangChain4j | 0.36.2 | AI 应用框架，统一抽象 |
| langchain4j-open-ai | 0.36.2 | 对接 DeepSeek API（OpenAI 兼容） |
| langchain4j-ollama | 0.36.2 | 本地嵌入模型（bge-m3:567m） |
| Qdrant | latest (Docker) | 向量数据库，存储知识库嵌入 |
| Spring WebFlux / Servlet | (SSE) | 流式输出到前端 |

## 核心配置

### LangChain4jConfig.java

```java
@Configuration
public class LangChain4jConfig {

    // 环境变量：DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL (默认 https://api.deepseek.com)
    // 环境变量：OLLAMA_BASE_URL (默认 http://localhost:11434)

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)        // DeepSeek API Key
            .baseUrl(baseUrl)      // https://api.deepseek.com/v1
            .modelName("deepseek-chat")
            .temperature(0.7)
            .maxTokens(2048)
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
            .baseUrl(ollamaBaseUrl)  // http://localhost:11434
            .modelName("bge-m3:567m")
            .timeout(Duration.ofSeconds(120))
            .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return QdrantEmbeddingStore.builder()
            .host(qdrantHost)         // localhost
            .port(6334)
            .collectionName("platform-knowledge")
            .build();
    }
}
```

### OpenAI 协议兼容性

DeepSeek API 完全兼容 OpenAI Chat Completion 协议，因此使用 `langchain4j-open-ai` 模块即可直接对接：

- **baseUrl**: `https://api.deepseek.com/v1`（注意尾部 `/v1`）
- **modelName**: `deepseek-chat`
- **认证**: `Authorization: Bearer sk-xxx`

## 流式对话 (SSE)

```java
// Controller
@PostMapping(value = "/api/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestBody ChatRequest request) {
    return aiChatService.chatStream(request.getSessionId(), request.getMessage());
}

// Service
public Flux<String> chatStream(Long sessionId, String userMessage) {
    // 1. 构建上下文（历史消息 + 知识库检索）
    String context = buildContext(sessionId, userMessage);

    // 2. 调用 DeepSeek 流式 API
    return Flux.create(sink -> {
        chatModel.chat(context, new StreamingResponseHandler<>() {
            public void onNext(String token) { sink.next(token); }
            public void onComplete() { sink.complete(); }
            public void onError(Throwable e) { sink.error(e); }
        });
    });
}
```

### 前端 SSE 接收

```typescript
// api/ai.ts
export function sendMessageStream(
  sessionId: number,
  message: string,
  onChunk: (chunk: string) => void,
  onComplete: () => void,
  onError: (err: any) => void
) {
  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ sessionId, message })
  }).then(async response => {
    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) { onComplete(); break }
      onChunk(decoder.decode(value, { stream: true }))
    }
  }).catch(onError)
}
```

## RAG 知识库

### 向量存储
- **Qdrant** 运行在 Docker 中（端口 6333/6334）
- **Collection**: `platform-knowledge`，dimension 1024（bge-m3 输出维度）
- **启动**: `docker-compose up -d qdrant`

### 知识入库
```
原始文档（开发文档.md、FAQ等）
    → 文本切片（500字/片，200字重叠）
    → Ollama Embedding（bge-m3:567m，本地 CPU 运行）
    → Qdrant 向量库
```

### 检索增强流程
```
用户提问 → Ollama Embedding → Qdrant 相似度搜索（Top 5）
    → 拼接 System Prompt（角色 + 检索知识 + 历史消息）
    → DeepSeek 生成回答
    → SSE 流式返回前端
```

### System Prompt 模板

```text
你是「组队星」平台的AI智能助手"小星"。你帮助大学生进行项目管理和竞赛组队。

回答规则：
1. 只回答与校园竞赛、项目管理、团队协作、平台使用相关的问题
2. 如果问题超出范围，礼貌地引导用户回到相关话题
3. 基于以下知识库内容回答，不要编造信息

知识库检索结果：
---
{retrievedChunks}
---

历史对话：
{conversationHistory}

用户问题：{userMessage}
```

## AI 对话数据模型

```sql
-- ai_session: 会话表
id, user_id, title, message_count, created_at, updated_at

-- ai_message: 消息表
id, session_id, role (user/assistant), content (text), created_at
```

## 智能推荐场景

### 竞赛推荐
基于用户技能标签 + 历史参赛记录 + 余弦相似度，推荐匹配的竞赛。

### 任务执行人推荐
基于成员技能 + 当前负载 + 任务复杂度，推荐最合适的成员。

### 导师推荐
基于团队研究方向 + 项目领域，匹配对应的导师。

## 开发检查清单

- [ ] DeepSeek API Key 配置在 `.env` 文件（不提交到 Git）
- [ ] Ollama 已安装并启动，`bge-m3:567m` 模型已下载
- [ ] Qdrant 容器运行正常，collection 已创建
- [ ] 流式接口设置了合理的超时时间（60s+）
- [ ] 对话历史有长度限制（最近 20 轮），超出时摘要压缩
- [ ] AI 输出内容经过 DOMPurify 清洗（前端）
- [ ] 错误降级：AI 不可用时返回友好提示而非 500
- [ ] Token 用量有监控日志

## 参考

- 后端开发见 [backend-development](../backend-development/SKILL.md)
- AI 聊天助手语气规范见 [ai-chat-assistant](../ai-chat-assistant/SKILL.md)
- 项目全貌见 [project-overview](../project-overview/SKILL.md)
