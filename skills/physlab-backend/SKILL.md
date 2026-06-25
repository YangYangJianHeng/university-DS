---
name: physlab-backend
description: >-
  Implements PhysLab Java Spring Cloud microservices including user center,
  resource library, simulation compute, data recording, reports, exams, and
  AI gateway. Use when building REST APIs, WebSocket endpoints, database
  models, or backend business logic for the physics platform.
---

# PhysLab 后端开发（Java + Spring Boot）

## 架构概览

微服务拆分，统一认证，独立部署：

| 服务 | 职责 |
|------|------|
| user-center | 用户、班级、角色权限 |
| resource-library | 实验资源、器材库、教学包 |
| simulation-compute | 电路 SPICE、力学/光学计算，WebSocket 推送 |
| data-recording | 操作日志、采集数据、回放 |
| report | 报告模板、提交、批改、查重 |
| exam | 考核、评分、防作弊 |
| ai-gateway | 多模型路由、RAG、流式对话 |

## 技术栈

- **框架**：Spring Boot 3 + Spring Cloud，JDK 17+
- **通信**：WebSocket + STOMP（仿真流、AI 流、课堂同步）
- **存储**：MySQL 8（业务）、MongoDB 6（日志/对话/非结构化）、Redis 7（实时状态）
- **消息**：RabbitMQ / Kafka（报告生成、日志分析、AI 批量任务）
- **部署**：Docker + K8s，Nginx 反向代理，Jenkins CI/CD

## 包结构约定

```
service-name/
├── src/main/java/com/physlab/{service}/
│   ├── controller/       # REST + STOMP 入口
│   ├── service/          # 业务逻辑
│   ├── domain/           # 实体、聚合
│   ├── repository/       # JPA / Mongo 仓储
│   ├── dto/              # 请求/响应 DTO
│   ├── config/           # 安全、WebSocket、MQ
│   └── client/           # 跨服务 Feign 调用
└── src/main/resources/
    ├── application.yml
    └── db/migration/     # Flyway/Liquibase
```

## 核心 API 域

### 实验资源

- CRUD：高中/大学实验库、自定义实验、多版本管理
- 教学包：预习测验、操作引导、审核上架
- 智能推荐：基于历史成绩与薄弱点推送实验

### 仿真会话

```
POST   /api/simulation/sessions          # 创建会话
WS     /ws/simulation/{sessionId}        # 状态推送
POST   /api/simulation/{id}/operations   # 操作事件批量上报
GET    /api/simulation/{id}/replay       # 回放时间轴
```

电路求解：自研轻量 SPICE，Java 端计算，结果经 WebSocket 推送前端。

### 数据与报告

- 采集点 CRUD、拟合结果存储、误差/不确定度计算（大学 A/B 类）
- 报告：模板渲染、LaTeX 校验、AI 预评语、查重
- 导出：Excel/CSV 异步任务经 MQ 处理

### 教学管理

- 班级、差异化发布（随机参数分组）、课堂同步模式
- 学情仪表板：知识点热力图、错误集中点、进度监控

### 考核评分

- 操作合规性：接线顺序、短路、量程错误扣分
- 数据真实性：偏离度校验
- 综合分：误差 + 操作规范 + 报告完整性 + 熟练度

## WebSocket 约定

| 目的地 | 方向 | 内容 |
|--------|------|------|
| `/app/simulation/{id}/operate` | C→S | 用户操作事件 |
| `/topic/simulation/{id}` | S→C | 仿真状态快照/增量 |
| `/app/ai/{id}/chat` | C→S | AI 提问（含上下文） |
| `/topic/ai/{id}/stream` | S→C | 流式回复 chunk |
| `/topic/classroom/{classId}` | S→C | 教师同步指令 |

消息体使用 JSON，包含 `timestamp`、`sessionId`、`payload` 字段。

## 数据模型要点

**MySQL**：用户、班级、实验元数据、报告、成绩、发布任务。

**MongoDB**：操作日志序列、对话记录、非结构化报告附件、回放事件索引。

**Redis**：会话实时状态、在线用户、课堂同步锁、热点缓存。

冷热分离：高频仿真中间态放 Redis，持久化快照异步写 MongoDB。

## 安全与隐私

- 统一 OAuth2/JWT 认证，服务间 mTLS 或内部 token。
- 敏感字段加密存储；传输 HTTPS/WSS。
- 生物特征（摄像头比对）需授权，默认本地处理不上云。
- 操作日志保留策略可配置，符合教育数据合规。

## 跨服务调用

- Feign + 熔断（Resilience4j）。
- 仿真计算可按实验类型（力/电/光）拆实例，负载均衡。
- AI 网关独立水平扩展，与业务服务解耦。

## 开发检查清单

- [ ] API 版本化（`/api/v1/`），DTO 与实体分离
- [ ] WebSocket 断线重连与会话恢复
- [ ] 数据库迁移脚本可重复执行
- [ ] 异步任务幂等（MQ 消费）
- [ ] 考核评分规则可配置（非硬编码）
- [ ] 日志含 traceId 便于链路追踪

## 参考

- 业务流程见 [开发文档.md](../../../开发文档.md) 第三、五节
- AI 网关详见 [physlab-ai](../physlab-ai/SKILL.md)
- 前端对接见 [physlab-frontend-ui](../physlab-frontend-ui/SKILL.md)
