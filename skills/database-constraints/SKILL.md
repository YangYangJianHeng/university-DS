---
name: database-constraints
description: >-
  「组队星」平台数据库约束与设计规范。涵盖 MySQL 8.0 数据表结构、字段命名规范、索引策略、
  外键关系、分页查询约定、MyBatis-Plus 映射规则。包含完整的 22 张核心表定义。
  Use when designing new tables, writing SQL migrations, defining entity mappings,
  or understanding data relationships across modules.
---

# 数据库约束与设计规范

## 基础规范

| 项目 | 规范 |
|------|------|
| 数据库 | MySQL 8.0 (InnoDB) |
| 字符集 | utf8mb4 / utf8mb4_unicode_ci |
| ORM | MyBatis-Plus 3.5.5 |
| 主键策略 | AUTO_INCREMENT（自增 ID） |
| 时间字段 | `created_at` / `updated_at` (DATETIME) |
| 软删除 | 逻辑删除字段 `deleted` (TINYINT, 0/1) |
| 状态字段 | 统一使用 ENUM 字符串或 TINYINT |

## 命名规范

- **表名**: 小写下划线，单数名词（`user` 而非 `users`）
- **字段名**: 小写下划线（`team_id` 而非 `teamId`）
- **主键**: 统一 `id` BIGINT AUTO_INCREMENT
- **外键**: `{关联表}_id`（如 `team_id`, `project_id`）
- **索引**: `idx_{表名}_{字段}`（如 `idx_user_phone`）
- **时间**: `created_at` / `updated_at`，MyBatis-Plus 自动填充

## 核心表结构

### 用户体系

```sql
-- user: 用户主表
CREATE TABLE user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20) UNIQUE,
  email VARCHAR(100),
  avatar VARCHAR(500),
  major VARCHAR(100),          -- 专业
  grade VARCHAR(20),           -- 年级
  introduction TEXT,           -- 个人简介
  role ENUM('USER','TEACHER','REVIEWER','ADMIN') DEFAULT 'USER',
  status TINYINT DEFAULT 1,   -- 1启用 0禁用
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- user_skill: 用户技能标签
CREATE TABLE user_skill (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  skill_name VARCHAR(100) NOT NULL,
  level VARCHAR(20),           -- BEGINNER/INTERMEDIATE/ADVANCED/MASTER
  INDEX idx_uskill_user (user_id)
);

-- user_experience: 用户项目经历
CREATE TABLE user_experience (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  project_name VARCHAR(200),
  role VARCHAR(100),
  description TEXT,
  start_date DATE,
  end_date DATE,
  INDEX idx_uexp_user (user_id)
);
```

### 团队体系

```sql
CREATE TABLE team (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  description TEXT,
  leader_id BIGINT NOT NULL,
  max_members INT DEFAULT 10,
  status ENUM('ACTIVE','DISBANDED','CLOSED') DEFAULT 'ACTIVE',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_team_leader (leader_id)
);

CREATE TABLE team_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  team_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) DEFAULT 'MEMBER',  -- LEADER/MEMBER
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tm_team_user (team_id, user_id)
);

CREATE TABLE team_teacher (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  team_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  status ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
  UNIQUE KEY uk_tt_team_teacher (team_id, teacher_id)
);
```

### 项目与任务

```sql
CREATE TABLE project (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  team_id BIGINT NOT NULL,
  status ENUM('PLANNING','IN_PROGRESS','REVIEW','COMPLETED','ARCHIVED') DEFAULT 'PLANNING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_project_team (team_id)
);

CREATE TABLE task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  priority ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM',
  status ENUM('TODO','DOING','DONE') DEFAULT 'TODO',
  deadline DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_task_project (project_id),
  INDEX idx_task_status (status),
  INDEX idx_task_deadline (deadline)
);

CREATE TABLE task_assignee (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  UNIQUE KEY uk_ta_task_user (task_id, user_id)
);

CREATE TABLE task_submission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT,
  file_urls JSON,              -- 附件URL数组
  status ENUM('SUBMITTED','ACCEPTED','REJECTED') DEFAULT 'SUBMITTED',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tsub_task (task_id)
);

CREATE TABLE task_comment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tc_task (task_id)
);
```

### 招募匹配

```sql
CREATE TABLE recruit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  publisher_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  project_type VARCHAR(50),     -- 竞赛/科研/创业
  skills_required JSON,         -- ["Java","Python","AI"]
  team_size INT,
  status ENUM('PENDING','APPROVED','REJECTED','CLOSED') DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recruit_publisher (publisher_id),
  INDEX idx_recruit_status (status)
);

CREATE TABLE application (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recruit_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message TEXT,
  status ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_app_recruit_user (recruit_id, user_id)
);
```

### 竞赛与导师

```sql
CREATE TABLE competition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  level ENUM('NATIONAL','PROVINCIAL','CITY','SCHOOL'),
  category ENUM('ACADEMIC','INNOVATION','ART','OTHER'),
  organizer VARCHAR(200),
  register_start DATE,
  register_end DATE,
  event_start DATE,
  event_end DATE,
  status ENUM('REGISTERING','UPCOMING','ONGOING','ENDED') DEFAULT 'REGISTERING',
  official_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teacher (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNIQUE,       -- 关联 user 表
  department VARCHAR(200),
  title VARCHAR(100),          -- 职称
  research_fields JSON,        -- ["人工智能","数据挖掘"]
  is_verified TINYINT DEFAULT 0,
  current_teams INT DEFAULT 0,
  INDEX idx_teacher_dept (department)
);
```

### 成果展品

```sql
CREATE TABLE achievement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  project_id BIGINT,
  team_id BIGINT,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  type ENUM('PAPER','PATENT','AWARD','SOFTWARE','OTHER'),
  award_level ENUM('NATIONAL','PROVINCIAL','SCHOOL'),
  file_urls JSON,
  is_public TINYINT DEFAULT 1,
  is_featured TINYINT DEFAULT 0,
  view_count INT DEFAULT 0,
  like_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ach_user (user_id),
  INDEX idx_ach_public (is_public)
);
```

### 通知消息

```sql
CREATE TABLE message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_user_id BIGINT,
  to_user_id BIGINT NOT NULL,
  type VARCHAR(50),            -- SYSTEM/JOIN_REQUEST/TASK_REMIND
  content TEXT,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_msg_to_user (to_user_id),
  INDEX idx_msg_read (is_read)
);

CREATE TABLE notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(50),
  title VARCHAR(200),
  content TEXT,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notif_user (user_id)
);
```

### AI 对话

```sql
CREATE TABLE ai_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200),
  message_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ais_user (user_id)
);

CREATE TABLE ai_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  role ENUM('user','assistant'),
  content TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_aim_session (session_id)
);
```

### 日历与审核

```sql
CREATE TABLE calendar_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200),
  event_date DATE,
  type VARCHAR(50),
  INDEX idx_cal_user (user_id)
);

CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  target_type VARCHAR(50),     -- RECRUIT/TEAM/ACHIEVEMENT
  target_id BIGINT,
  action ENUM('APPROVE','REJECT'),
  reason TEXT,
  reviewer_id BIGINT,
  before_status VARCHAR(50),
  after_status VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) UNIQUE,
  config_value TEXT,
  description VARCHAR(500)
);
```

## MyBatis-Plus 映射规则

### Entity 标准写法

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String phone;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### 自动填充配置

`created_at` 和 `updated_at` 由 `MyMetaObjectHandler` 自动填充，无需手动设置。

### 分页查询

```java
// Service 层
Page<Entity> page = new Page<>(pageNum, pageSize);
LambdaQueryWrapper<Entity> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Entity::getStatus, "ACTIVE").orderByDesc(Entity::getCreatedAt);
mapper.selectPage(page, wrapper);
```

## 索引策略

| 字段类型 | 索引策略 |
|----------|----------|
| 外键字段 | 必须建索引（`team_id`, `user_id`, `project_id`） |
| 状态字段 | 高频查询字段建索引（`status`, `is_read`） |
| 时间字段 | 排序字段建索引（`created_at`） |
| 唯一约束 | 业务唯一字段建 UNIQUE KEY（`phone`, 关联组合） |

## 生成 SQL 文件约定

- 存放位置: `src/main/resources/sql/`
- 文件命名: `V{序号}__{描述}.sql`（如 `V1__init.sql`, `V2__ai_tables.sql`）
- 必须可重复执行，使用 `CREATE TABLE IF NOT EXISTS`

## 开发检查清单

- [ ] 新表字段命名符合下划线规范
- [ ] 外键字段已建索引
- [ ] 时间字段使用 DATETIME（非 TIMESTAMP）
- [ ] Entity 类正确映射 @TableName 和 @TableId
- [ ] SQL 脚本可重复执行（IF NOT EXISTS）
- [ ] 敏感字段（密码）已使用 BCrypt 加密存储
- [ ] 未使用物理外键（逻辑外键即可，应用层维护）
