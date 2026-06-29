# 组队星 — Agent Skills

基于项目实际代码和 [开发文档.md](../开发文档.md) 生成的项目级 Agent Skills，供 AI 编码助手自动加载领域知识，精准理解用户需求并生成高质量代码。

## Skills 列表

| Skill | 目录 | 适用场景 |
|-------|------|----------|
| 项目全貌 | [project-overview](./project-overview/SKILL.md) | 新人上手、架构理解、技术栈导航 |
| 后端开发 | [backend-development](./backend-development/SKILL.md) | Spring Boot API、Service/Mapper、安全认证、WebSocket |
| 前端开发 | [frontend-development](./frontend-development/SKILL.md) | Vue3 组件、Element Plus、样式规范、路由、状态管理 |
| 数据库约束 | [database-constraints](./database-constraints/SKILL.md) | 建表 SQL、Entity 映射、索引策略、分页查询 |
| AI 集成 | [ai-integration](./ai-integration/SKILL.md) | LangChain4j、DeepSeek、Ollama、Qdrant RAG、流式 SSE |
| AI 聊天助手 | [ai-chat-assistant](./ai-chat-assistant/SKILL.md) | 15+ 场景回答策略、意图识别、情感感知、多轮对话 |
| 提示词理解 | [prompt-understanding](./prompt-understanding/SKILL.md) | 需求澄清、歧义消除、任务拆解、技术约束推断 |

## 使用方式

AI 编码助手会根据各 Skill 的 `description` 自动判断是否加载。也可在对话中明确提及：

- 「按后端开发 skill 添加一个竞赛报名的 API」
- 「参考前端开发 skill 实现任务看板页面」
- 「按数据库约束 skill 设计新的通知表」
- 「按 AI 聊天助手 skill 优化欢迎语回复」

## 维护原则

- 当项目技术栈或架构变更时，同步更新对应 Skill
- 新增模块时，评估是否需要创建新 Skill
- 每个 Skill 聚焦一个领域，避免内容交叉冗余
- 所有 Skill 间通过「参考」节建立关联导航
