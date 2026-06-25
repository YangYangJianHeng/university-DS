---
name: physlab-ai
description: >-
  Integrates AI capabilities for PhysLab using Spring AI, Ollama, cloud LLMs,
  RAG with Milvus, and WebSocket streaming. Use when building experiment Q&A,
  operation guidance, report grading, fault diagnosis, lesson plan generation,
  or AI gateway routing.
---

# PhysLab AI 智能辅助开发

## AI 能力矩阵

| 场景 | 输入 | 输出 |
|------|------|------|
| 实验智能问答 | 学生问题 + 实验上下文 | 原理/操作解答（文本/语音） |
| 操作实时指导 | 当前步骤、电路/器材状态 | 下一步提示或讲解 |
| 智能批改 | 报告 + 操作记录 + 数据误差 | 综合评语与改进建议 |
| 学习路径规划 | 知识点图谱 + 历史表现 | 实验序列、微课推送 |
| 故障诊断 | 异常数据 + 操作日志 | 可能原因与排查步骤 |
| 教案生成 | 教师关键词/教学目标 | 完整教案（目标/原理/步骤/评分） |
| AI 备课助手 | 教学目标 | 实验组合 + 差异化参数 |

## 架构

```
Frontend (WebSocket)
       ↓
  ai-gateway (Spring AI)
       ├── Model Router (本地 Ollama / 云端 OpenAI·文心等)
       ├── Context Builder (实验状态 + 会话历史)
       ├── RAG Retriever (Milvus 向量库)
       └── Function Calling (查询实验数据/API)
       ↓
  LLM Provider
```

## 技术选型

- **框架**：Spring AI，统一抽象多模型调用
- **本地模型**：Ollama + Llama3 / Qwen2（7B/14B，约 8–16GB 显存）
- **云端**：OpenAI GPT-4o、文心一言等，可热切换
- **向量库**：Milvus，存储实验手册、教材切片、器材说明
- **通信**：WebSocket 流式响应至前端

## 上下文拼装

每次 AI 请求必须携带：

1. `sessionId` — 实验会话
2. `experimentId` — 实验类型与手册引用
3. `currentState` — 电路连接、仪器读数、已完成步骤
4. `collectedData` — 已记录数据点与拟合结果
5. `mode` — demo / practice / exam（考核模式限制提示粒度）
6. `conversationHistory` — 最近 N 轮对话

```java
// Prompt 结构示意
var context = ExperimentContext.builder()
    .sessionId(sessionId)
    .circuitTopology(state.getConnections())
    .recentErrors(replayService.getErrors(sessionId))
    .dataPoints(dataService.getPoints(sessionId))
    .build();

var ragChunks = vectorStore.similaritySearch(question, 5);
var prompt = promptTemplate.render(context, ragChunks, question);
```

## RAG 知识库

**入库文档**：实验原理、常见问题 FAQ、器材手册、标准步骤、评分 rubric。

**流程**：
1. 文档切片（按章节/器材/实验 ID 打 tag）
2. Embedding 写入 Milvus
3. 检索时按 `experimentId` 过滤 + 相似度排序
4. 注入 system prompt，确保回答可控、可溯源

## Function Calling

允许模型调用内部 API 获取实时数据，避免幻觉：

- `getCircuitState(sessionId)`
- `getDataPoints(sessionId)`
- `getStandardValue(experimentId, param)`
- `getCommonMistakes(experimentId)`

## 流式对话

```
Client → /app/ai/{sessionId}/chat  { "message": "...", "voice": false }
Server → /topic/ai/{sessionId}/stream  (SSE-like chunks via STOMP)
Server → /topic/ai/{sessionId}/done    { "fullText": "...", "sources": [...] }
```

前端：打字机效果 + 来源引用折叠；语音模式接 TTS。

## 各场景 Prompt 要点

### 实验问答
- 结合当前接法回答（如分压式 vs 限流式）。
- 考核模式不直接给出答案，引导思考。

### 操作指导
- 练习模式可主动提示；演示模式仅解说。
- 检测重复错误时升级提示强度。

### 智能批改
- 输入：报告正文、数据表格、操作回放摘要、误差分析。
- 输出：分项评分建议 + 可编辑评语模板。
- 教师端一键采纳/修改。

### 故障诊断
- 对比理论曲线、异常点、操作错误时间线。
- 输出有序排查清单（接线 → 器材 → 读数 → 环境）。

## 安全与合规

- 对话记录存 MongoDB，保留策略可配置。
- 不向云端发送未授权生物特征数据。
- 考核模式 AI 功能可全局或按班级关闭。
- 模型输出过滤：禁止危险实验操作建议。

## 开发检查清单

- [ ] 上下文长度控制，超长时摘要压缩
- [ ] RAG 结果带 experimentId 过滤
- [ ] 流式中断与取消（用户停止生成）
- [ ] 本地/云端模型 failover
- [ ] Token 用量与延迟监控
- [ ] 考核模式 prompt 策略与普通模式分离

## 参考

- AI 模块设计见 [开发文档.md](../../../开发文档.md) 第四节、业务流程第三节
- 网关与 WebSocket 见 [physlab-backend](../physlab-backend/SKILL.md)
- 前端 AI UI 见 [physlab-frontend-ui](../physlab-frontend-ui/SKILL.md)
