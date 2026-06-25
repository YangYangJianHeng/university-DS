# PhysLab Cursor Skills

基于 [开发文档.md](../../开发文档.md) 生成的项目级 Agent Skills，供 Cursor 在开发物理实验仿真平台时自动加载领域知识。

## Skills 列表

| Skill | 目录 | 适用场景 |
|-------|------|----------|
| 前端 UI 开发 | [physlab-frontend-ui](./physlab-frontend-ui/SKILL.md) | 3D 场景、图表、Web/移动端/Electron UI |
| 后端开发 | [physlab-backend](./physlab-backend/SKILL.md) | Spring Cloud 微服务、API、WebSocket、存储 |
| 仿真引擎 | [physlab-simulation](./physlab-simulation/SKILL.md) | 电路/力学/光学/热学仿真与状态同步 |
| **3D 建模** | [physlab-3d-modeling](./physlab-3d-modeling/SKILL.md) | Three.js 器材建模、导线、特效 |
| AI 智能辅助 | [physlab-ai](./physlab-ai/SKILL.md) | Spring AI、RAG、流式问答、智能批改 |
| 实验业务域 | [physlab-experiment-domain](./physlab-experiment-domain/SKILL.md) | 资源库、报告、教学管理、考核评分 |

## 使用方式

Cursor 会根据各 Skill 的 `description` 自动判断是否加载。也可在对话中明确提及，例如：

- 「按 physlab-frontend-ui skill 实现示波器组件」
- 「参考仿真引擎 skill 添加短路检测逻辑」

## 维护

当 [开发文档.md](../../开发文档.md) 更新时，同步修订对应 Skill 内容，保持技术选型与业务流程一致。
