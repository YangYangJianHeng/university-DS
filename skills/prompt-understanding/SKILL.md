---
name: prompt-understanding
description: >-
  提示词理解与优化规范。帮助 AI 精确理解用户意图，将模糊需求转化为明确的技术任务。
  涵盖需求澄清、歧义消除、上下文推断、任务拆解策略。
  Use when user requests are ambiguous, need clarification, or when the AI needs to
  infer unstated requirements and convert natural language into actionable development tasks.
---

# 提示词理解与优化规范

## 核心原则

**目标**: 将用户的自然语言需求精准转化为可执行的开发任务，减少来回确认，提高一次回答命中率。

## 1. 需求理解策略

### 1.1 优先级推断

当用户同时提出多个需求时，按以下优先级排序：

| 优先级 | 类型 | 示例 |
|--------|------|------|
| P0 致命 | 阻塞性问题 | 端口占用、编译报错、服务启动失败 |
| P1 高优 | 核心功能缺失 | API 报 500、页面白屏、数据丢失 |
| P2 中优 | 体验优化 | UI 调整、动画效果、加载速度 |
| P3 低优 | 锦上添花 | 颜色微调、文案优化、可选功能 |

### 1.2 隐含需求识别

用户说出来的只是冰山一角，需要推断：

| 用户说 | 可能隐含的需求 |
|--------|----------------|
| "优化一下性能" | 需要先分析瓶颈 → 定位慢查询/N+1 → 添加缓存/索引 |
| "加个登录" | 需要注册页 + 登录页 + JWT + 路由守卫 + 密码加密 |
| "写个接口" | 需要 Controller + Service + Mapper + 参数校验 + 异常处理 + 文档 |
| "改下样式" | 需要保持现有组件库风格一致（Element Plus 主题色） |
| "AI 聊天不够智能" | 需要优化 Prompt 模板 + 增强知识库 + 改进上下文拼接 |

### 1.3 技术栈约束自动识别

在本项目中，自动应用以下约束：
- **后端**: Spring Boot 3.2 + JDK 17 + MyBatis-Plus（不引入 JPA/Hibernate）
- **前端**: Vue 3 Composition API + TypeScript + Element Plus（不引入其他 UI 库）
- **AI**: LangChain4j + DeepSeek（不引入 Spring AI 或其他框架）
- **数据库**: MySQL 8.0（不引入 PostgreSQL/MongoDB 等新库）
- **样式**: Scoped CSS + Element Plus 变量（不引入 Tailwind/Bootstrap）

## 2. 歧义消除策略

### 2.1 确认级别

- **直接执行**: 改动范围小、无歧义（如删除文件、修改单个值）
- **推断后执行**: 改动范围中等，根据上下文可以推断（如添加 CRUD 接口）
- **追问确认**: 涉及架构决策或用户未提供足够信息

### 2.2 典型歧义场景

| 歧义场景 | 消歧方法 |
|----------|----------|
| "修改一下" | 确认具体文件/页面，确认期望效果 |
| "不太好看" | 推断：对齐主流设计风格，参考现有页面一致 |
| "加个功能" | 拆解为：数据表 → 后端接口 → 前端页面 |
| "部署上去" | 确认目标环境（Docker/本地/云服务器） |
| "和XX一样" | 找到参考页面，复用组件结构和样式 |

## 3. 任务拆解策略

### 3.1 全栈任务标准拆解

```
用户需求
  ├── 数据库层: 建表 SQL → Entity → Mapper
  ├── 后端层:   DTO → Service → Controller → 单元测试
  ├── 前端层:   API 封装 → 页面组件 → 路由配置
  └── 联调:    前后端联通验证
```

### 3.2 前端任务拆解

```
页面需求
  ├── API 层: request.ts 添加接口方法
  ├── 状态层: 定义 ref/reactive 响应式数据
  ├── 模板层: Element Plus 组件 + v-for/v-if
  ├── 样式层: scoped CSS + 响应式适配
  └── 交互层: loading/empty/error 三态处理
```

### 3.3 后端任务拆解

```
接口需求
  ├── Entity: 数据库实体 + @TableName + @TableField
  ├── Mapper: 继承 BaseMapper<Entity>
  ├── Service: 接口定义 + Impl 实现（@Transactional 事务）
  ├── Controller: @RestController + 参数校验 + 统一响应
  └── 文档: Knife4j 自动生成（无需手动写文档）
```

## 4. 上下文自动注入

在回答时自动引用以下信息（无需用户明确提及）：

- **当前项目**: `university-DS`（大学生组队平台），而非 PhysLab 或其他项目
- **工作空间**: `d:/JavaProjects/java_springboot_project/university-DS`
- **项目包名**: `com.platform`，前端路由基于 `Layout.vue`
- **端口**: 后端 8080，前端 5173
- **数据库**: MySQL 8.0，Redis 6379，Qdrant 6333/6334

## 5. 答案质量标准

### 5.1 好回答的特征

- [ ] 代码可直接复制粘贴运行，无需额外修改
- [ ] 引入了正确的 import 语句
- [ ] 样式与现有页面风格一致
- [ ] 包含错误处理和边界情况
- [ ] 说明了修改哪些文件（路径明确）
- [ ] 中文回复，简洁直接

### 5.2 避免的行为

- [ ] 输出大量无关代码替代简洁编辑
- [ ] 引入项目未使用的第三方库
- [ ] 重写整个文件替代精准修改
- [ ] 使用过时的 API 或语法
- [ ] 忽略用户在对话中已提供的上下文

## 6. 特殊术语映射

| 用户可能说的 | 对应项目中的概念 |
|-------------|-----------------|
| "聊天" / "对话" / "AI" | `AiChat.vue` + `ai/` 模块 |
| "团队" / "队伍" | `team/` 模块 + `TeamList/TeamDetail.vue` |
| "项目" / "课题" | `project/` 模块 + `ProjectDetail.vue` |
| "任务" / "待办" / "看板" | `task/` 模块 + `TaskList.vue` |
| "比赛" / "竞赛" / "大赛" | `competition/` 模块 |
| "招募" / "招人" / "找人" | `recruit/` 模块 |
| "导师" / "老师" / "指导" | `teacher/` 模块 |
| "成果" / "获奖" / "证书" | `achievement/` + `badge/` + `certificate/` |
| "后台" / "管理" / "审核" | `admin/AdminDashboard.vue` |
| "小星" | AI 智能助手的名字 |
