---
name: backend-development
description: >-
  「组队星」Spring Boot 3.2 后端开发规范。涵盖项目架构、MyBatis-Plus ORM、MySQL/Redis 数据存储、
  Spring Security 6 + JWT 认证、WebSocket 实时通讯、统一响应格式、全局异常处理。
  Use when building REST APIs, database models, service logic, security config,
  or any backend feature for the university team-building platform.
---

# 后端开发规范

## 项目架构

```
com.platform
├── PlatformApplication.java          # Spring Boot 启动类
├── common/                           # 通用层
│   ├── annotation/                   # @RateLimit 等自定义注解
│   ├── aspect/                       # AOP 切面（限流、日志）
│   ├── config/                       # 全局配置（Knife4j、MyBatis-Plus、CORS）
│   ├── exception/                    # 全局异常处理 + 业务异常枚举
│   └── util/                         # 工具类（JWT、断言等）
├── config/                           # Security、WebSocket 专项配置
├── module/                           # 业务模块（按领域拆分）
│   ├── user/                         # 用户管理
│   ├── team/                         # 团队管理
│   ├── project/                      # 项目管理
│   ├── task/                         # 任务协作
│   ├── recruit/                      # 招募匹配
│   ├── competition/                  # 竞赛信息
│   ├── achievement/                  # 成果展厅
│   ├── teacher/                      # 导师对接
│   ├── message/                      # 通知消息
│   ├── calendar/                     # 竞赛日历
│   ├── admin/                        # 管理后台
│   ├── file/                         # 文件中心
│   └── ai/                           # AI 智能助手
│       ├── config/                   # LangChain4j 配置（DeepSeek/Ollama）
│       ├── controller/
│       ├── service/                  # AI 对话服务（含流式 SSE）
│       └── dto/
└── resources/
    ├── application.yml               # 主配置
    ├── application-dev.yml           # 开发环境
    ├── application-prod.yml          # 生产环境
    └── sql/                          # 数据库初始化脚本
```

## 技术栈版本

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| JDK | 17 | 运行环境 |
| MyBatis-Plus | 3.5.5 | ORM + 分页 |
| MySQL | 8.0 | 业务数据存储 |
| Redis | 7.x | 缓存 + 分布式锁 |
| JJWT | 0.12.3 | JWT 令牌 |
| Knife4j | 4.4.0 | API 文档 |
| LangChain4j | 0.36.2 | AI Agent 框架 |
| EasyExcel | 3.3.3 | Excel 读写 |
| Lombok | 1.18.30 | 代码简化 |

## 统一响应格式

```java
// 所有 API 返回统一包装
{
  "code": 200,        // HTTP 状态码
  "message": "success",
  "data": { ... },    // 业务数据
  "timestamp": 1719000000000
}
```

## API 设计规范

- **路径前缀**: 模块级路径，如 `/api/team/`, `/api/project/`, `/api/task/`
- **版本**: 当前为 v1，路径不显式带版本号
- **命名**: RESTful 风格，复数资源名
- **分页**: 统一使用 MyBatis-Plus 的 `IPage`，参数 `page`/`size`
- **认证**: `Authorization: Bearer <jwt_token>` 头

## 全局异常处理

使用 `@RestControllerAdvice` 统一处理：
- 参数校验失败 → 400 + 具体字段错误
- 认证失败 → 401
- 权限不足 → 403
- 业务异常 → 对应业务错误码
- 未知异常 → 500 + 日志记录

## JWT 认证流程

1. 用户登录 → 返回 accessToken（2h 有效期）+ refreshToken（7天）
2. 每次请求携带 `Authorization: Bearer <token>`
3. Security 过滤器链拦截验证
4. 从 token 中提取 userId、role 注入 SecurityContext

## 模块开发标准模板

每个业务模块目录下：
```
module/{domain}/
├── controller/    # @RestController，薄层，只做参数校验和调用 service
├── service/
│   ├── {Xxx}Service.java      # 接口
│   └── impl/{Xxx}ServiceImpl.java  # 实现
├── mapper/        # MyBatis-Plus BaseMapper 扩展
├── entity/        # 数据库实体（@TableName）
├── dto/           # 请求/响应 DTO
└── vo/            # 视图对象（可选）
```

## WebSocket 实时通讯

- 使用原生 `@ServerEndpoint` 注解
- 端点：`/ws/chat/{userId}`
- 消息体 JSON，含 `type`、`content`、`from`、`to`、`timestamp`
- 支持心跳保活与断线重连

## Redis 使用场景

- **缓存**: 竞赛列表、用户画像、推荐结果（TTL 30min-2h）
- **分布式锁**: 防止重复操作（如报名竞赛超卖）
- **在线状态**: WebSocket 用户在线标识
- **限流**: 结合 `@RateLimit` 注解，Redis 计数器

## 开发检查清单

- [ ] 新模块是否遵循统一的 service → impl → mapper 分层
- [ ] API 返回统一响应格式（不直接返回实体）
- [ ] 敏感操作有权限校验（@PreAuthorize 或手动校验）
- [ ] 数据库操作考虑事务（@Transactional）
- [ ] 外部 API 调用有超时和异常兜底
- [ ] 日志含关键参数（用户ID、操作类型）
- [ ] 异常有明确的中文错误提示

## 参考

- 数据库设计见 [database-constraints](../database-constraints/SKILL.md)
- AI 集成见 [ai-integration](../ai-integration/SKILL.md)
- 前端对接见 [frontend-development](../frontend-development/SKILL.md)
- 项目全貌见 [project-overview](../project-overview/SKILL.md)
