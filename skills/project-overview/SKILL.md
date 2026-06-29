---
name: project-overview
description: >-
  「组队星」大学生竞赛/科研项目组队匹配与过程管理平台——项目全貌概览。
  涵盖架构设计、13大功能模块、技术栈总览、端口配置、目录结构、环境变量、启动方式。
  Use when onboarding a new developer, understanding system architecture, or
  navigating the codebase for the first time.
---

# 项目全貌概览

## 项目定位

**「组队星」** —— 大学生竞赛/科研项目组队匹配与过程管理平台。

将志同道合的同学、经验丰富的导师、优质的竞赛资源连接在一起，一站式完成：

```
找人（招募匹配）→ 建队（团队管理）→ 干事（项目+任务协作）
    → 参赛（竞赛信息）→ 请老师（导师对接）→ 秀成果（成果展厅）
```

## 系统架构

```
┌─────────────────────────────────────────────┐
│                  前端层                      │
│   Vue 3 + Element Plus + Vite (Port 5173)   │
└──────────────────┬──────────────────────────┘
                   │ HTTP REST / SSE / WebSocket
┌──────────────────┴──────────────────────────┐
│               后端应用层 (Port 8080)         │
│         Spring Boot 3.2 + JDK 17            │
│   ┌──────┬──────┬──────┬──────┬──────┐     │
│   │用户  │团队  │项目  │任务  │招募  │     │
│   ├──────┼──────┼──────┼──────┼──────┤     │
│   │竞赛  │导师  │成果  │文件  │消息  │     │
│   ├──────┼──────┼──────┼──────┼──────┤     │
│   │日历  │AI助手│管理  │徽章  │奖状  │     │
│   └──────┴──────┴──────┴──────┴──────┘     │
└──────────────────┬──────────────────────────┘
      ┌────────────┼────────────┐
      ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  MySQL 8 │ │  Redis 7 │ │  Qdrant  │
│  业务库  │ │ 缓存/锁  │ │ 向量库   │
└──────────┘ └──────────┘ └──────────┘
                    ┌──────────┐
                    │  Ollama  │
                    │本地嵌入模型│
                    └──────────┘
```

## 端口分配

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | Spring Boot 应用 | 后端 API 服务 |
| 5173 | Vite Dev Server | 前端开发服务器 |
| 3306 | MySQL 8.0 | 关系数据库 |
| 6379 | Redis 7.x | 缓存 + 分布式锁 |
| 11434 | Ollama | 本地嵌入模型 API |
| 6333/6334 | Qdrant | 向量数据库 (HTTP/gRPC) |

## 技术栈总览

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| JDK | 17 | 运行环境 |
| MyBatis-Plus | 3.5.5 | ORM |
| MySQL | 8.0 | 关系数据库 |
| Redis | 7.x | 缓存/锁 |
| Spring Security 6 | 6.x | 认证授权 |
| JJWT | 0.12.3 | JWT 令牌 |
| Knife4j | 4.4.0 | API 文档 |
| LangChain4j | 0.36.2 | AI 框架 |
| EasyExcel | 3.3.3 | Excel 处理 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| marked | latest | Markdown 渲染 |
| DOMPurify | latest | XSS 防护 |

### AI
| 服务 | 说明 |
|------|------|
| DeepSeek API | 云端 LLM（OpenAI 兼容协议） |
| Ollama bge-m3 | 本地嵌入模型（1024维向量） |
| Qdrant | 向量数据库（RAG 语义检索） |

## 13 大功能模块

| # | 模块 | 核心功能 | 对应包/目录 |
|---|------|----------|-------------|
| 1 | 用户管理 | 注册/登录/画像/RBAC | `module/user/` |
| 2 | 招募匹配 | 发布/浏览/智能匹配/申请/推荐 | `module/recruit/` |
| 3 | 团队管理 | 创建/成员/解散/动态墙 | `module/team/` |
| 4 | 项目管理 | 立项/里程碑/结题/风险评估 | `module/project/` |
| 5 | 任务协作 | 创建/指派/看板/验收/评论 | `module/task/` |
| 6 | 竞赛中心 | 竞赛库/详情/推荐/报名 | `module/competition/` |
| 7 | 导师对接 | 入驻/检索/邀请/指导记录 | `module/teacher/` |
| 8 | 成果展厅 | 提交/审核/佳作/点赞收藏 | `module/achievement/` |
| 9 | 文件中心 | 上传/文件夹/权限/搜索 | `module/file/` |
| 10 | 消息通知 | 站内通知/延迟消息 | `module/message/` |
| 11 | 竞赛日历 | 日历视图/冲突检测/提醒 | `module/calendar/` |
| 12 | AI 助手 | 智能问答/流式对话/RAG | `module/ai/` |
| 13 | 管理后台 | 用户/审核/数据看板/徽章 | `module/admin/` |

## 环境配置

### .env 文件（项目根目录）

```bash
# 数据库
DB_HOST=localhost
DB_PORT=3306
DB_NAME=university_platform
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# DeepSeek AI
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
DEEPSEEK_BASE_URL=https://api.deepseek.com

# Ollama
OLLAMA_BASE_URL=http://localhost:11434

# 文件上传
UPLOAD_DIR=./uploads
```

### 启动流程

```bash
# 1. 启动中间件
docker-compose -f docker-compose-qdrant.yml up -d  # Qdrant
# MySQL 和 Redis 需手动安装或 Docker 启动

# 2. 启动 Ollama（需先安装 Ollama）
ollama serve
ollama pull bge-m3:567m

# 3. 启动后端
cd university-DS
mvn spring-boot:run

# 4. 启动前端
cd frontend
npm install
npm run dev
```

## 项目目录树

```
university-DS/
├── pom.xml                         # Maven 父 POM
├── .env                            # 环境变量（不提交 Git）
├── docker-compose-qdrant.yml       # Qdrant 容器配置
├── 开发文档.md                      # 产品/技术文档（V2.0）
├── uploads/                        # 文件上传目录
├── qdrant_data/                    # Qdrant 数据持久化
├── skills/                         # Agent Skills（本项目）
│   ├── README.md
│   ├── ai-chat-assistant/          # AI 聊天语气规范
│   ├── ai-integration/             # AI 集成规范
│   ├── backend-development/        # 后端开发规范
│   ├── database-constraints/       # 数据库约束
│   ├── frontend-development/       # 前端开发规范
│   ├── project-overview/           # 本文件 - 项目全貌
│   └── prompt-understanding/       # 提示词理解
├── src/                            # 后端源码
│   ├── main/java/com/platform/     # Java 源码（13个业务模块）
│   └── main/resources/             # 配置 + SQL 脚本
└── frontend/                       # 前端源码
    └── src/
        ├── api/                    # API 封装
        ├── components/             # 通用组件
        ├── router/                 # 路由配置
        ├── views/                  # 页面组件（20+）
        └── stores/                 # Pinia 状态
```

## 关键约定

- **统一响应**: `{ code, message, data, timestamp }`
- **认证方式**: JWT Bearer Token
- **API 文档**: http://localhost:8080/doc.html (Knife4j)
- **页面入口**: 所有需要登录的页面由 `Layout.vue` 包裹
- **样式风格**: Element Plus 默认主题 + 蓝紫色渐变点缀
- **代码生成**: 所有 Entity/Mapper 手动编写，不使用代码生成器

## 参考

- 后端详细规范 → [backend-development](../backend-development/SKILL.md)
- 前端详细规范 → [frontend-development](../frontend-development/SKILL.md)
- 数据库表结构 → [database-constraints](../database-constraints/SKILL.md)
- AI 集成 → [ai-integration](../ai-integration/SKILL.md)
- AI 聊天语气 → [ai-chat-assistant](../ai-chat-assistant/SKILL.md)
- 提示词理解 → [prompt-understanding](../prompt-understanding/SKILL.md)
