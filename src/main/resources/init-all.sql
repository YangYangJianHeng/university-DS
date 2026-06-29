-- =============================================
-- 大学生竞赛/科研项目组队匹配与过程管理平台
-- 完整数据库初始化脚本（MySQL）
-- 
-- 整合了以下文件：
--   - mysql-init.sql（建库）
--   - schema.sql（建表）
--   - init-all.sql（示例数据）
--   - migration*.sql（变更已合并到建表语句）
-- 
-- 使用方式：
--   mysql -u root -p < init-all.sql
--   或直接在 Navicat/DataGrip 中执行
-- =============================================

CREATE DATABASE IF NOT EXISTS `university_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `university_platform`;

-- =============================================
-- 1. 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phone` VARCHAR(11) UNIQUE NOT NULL,
    `student_id` VARCHAR(20) UNIQUE,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `major` VARCHAR(100),
    `grade` VARCHAR(10),
    `avatar` VARCHAR(255),
    `bio` VARCHAR(500),
    `role` VARCHAR(20) DEFAULT 'USER',
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 2. 用户技能表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_skill` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `skill_name` VARCHAR(50) NOT NULL,
    `level` VARCHAR(20) DEFAULT 'BEGINNER',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 3. 用户经历表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_experience` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `project_id` BIGINT,
    `role` VARCHAR(50),
    `achievement` VARCHAR(500),
    `started_at` DATE,
    `ended_at` DATE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 4. 招募表
-- =============================================
CREATE TABLE IF NOT EXISTS `recruit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `publisher_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `project_icon` VARCHAR(500) COMMENT '项目图标URL',
    `project_name` VARCHAR(200) COMMENT '项目名称',
    `description` TEXT,
    `project_type` VARCHAR(50),
    `required_skills` JSON,
    `max_members` INT DEFAULT 5,
    `current_members` INT DEFAULT 1,
    `deadline` DATETIME,
    `status` VARCHAR(20) DEFAULT 'OPEN',
    `audit_status` VARCHAR(20) DEFAULT 'PENDING',
    `view_count` INT DEFAULT 0,
    `project_id` BIGINT COMMENT '关联的项目ID（转为项目后填充）',
    `team_id` BIGINT COMMENT '关联的团队ID（转为项目后填充）',
    `continue_recruit` TINYINT DEFAULT 0 COMMENT '转为项目后是否继续招募：0=停止，1=继续',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`publisher_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 5. 申请记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recruit_id` BIGINT NOT NULL,
    `applicant_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `applied_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`recruit_id`) REFERENCES `recruit`(`id`),
    FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 6. 团队表
-- =============================================
CREATE TABLE IF NOT EXISTS `team` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT COMMENT '团队描述',
    `leader_id` BIGINT NOT NULL,
    `recruit_id` BIGINT,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`recruit_id`) REFERENCES `recruit`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 7. 团队成员表
-- =============================================
CREATE TABLE IF NOT EXISTS `team_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` VARCHAR(20) DEFAULT 'MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 8. 团队-导师关联表
-- =============================================
CREATE TABLE IF NOT EXISTS `team_teacher` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `message` VARCHAR(500) COMMENT '邀请留言信息',
    `invited_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `approved_at` DATETIME,
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`),
    FOREIGN KEY (`teacher_id`) REFERENCES `teacher`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 9. 项目表
-- =============================================
CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL,
    `leader_id` BIGINT NOT NULL COMMENT '项目负责人',
    `name` VARCHAR(200) NOT NULL,
    `icon` VARCHAR(500) COMMENT '项目图标URL',
    `description` TEXT,
    `milestones` TEXT COMMENT '里程碑JSON数组',
    `status` VARCHAR(20) DEFAULT 'ONGOING',
    `started_at` DATE,
    `ended_at` DATE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`),
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 10. 任务表（v2.0：多人指派）
-- =============================================
CREATE TABLE IF NOT EXISTS `task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` BIGINT NOT NULL,
    `creator_id` BIGINT NOT NULL COMMENT '任务创建者（项目负责人）',
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `required_skills` VARCHAR(500) COMMENT '任务所需技能，逗号分隔',
    `priority` VARCHAR(10) DEFAULT 'MEDIUM',
    `status` VARCHAR(20) DEFAULT 'TODO' COMMENT 'TODO/IN_PROGRESS/SUBMITTED/REVIEWING/DONE/OVERDUE',
    `deadline` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`project_id`) REFERENCES `project`(`id`),
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 11. 任务执行人关联表（多人指派）
-- =============================================
CREATE TABLE IF NOT EXISTS `task_assignee` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `assignment_note` VARCHAR(500) COMMENT '负责人对该执行人的分工说明',
    `skill_match_score` DECIMAL(3,2) DEFAULT 0 COMMENT '技能匹配度（0-1）',
    `status` VARCHAR(20) DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED/ACCEPTED/REJECTED',
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `accepted_at` DATETIME,
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_task_user` (`task_id`, `user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 12. 任务成果提交表（阶段性成果）
-- =============================================
CREATE TABLE IF NOT EXISTS `task_submission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `submitter_id` BIGINT NOT NULL COMMENT '提交人',
    `title` VARCHAR(200) NOT NULL COMMENT '成果标题',
    `description` TEXT COMMENT '成果描述',
    `file_urls` JSON COMMENT '附件URL列表（图片/视频/压缩包/文档）',
    `status` VARCHAR(20) DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/APPROVED/REJECTED',
    `review_comment` TEXT COMMENT '验收意见',
    `reviewer_id` BIGINT COMMENT '验收人（项目负责人）',
    `reviewed_at` DATETIME COMMENT '验收时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 13. 任务评论表
-- =============================================
CREATE TABLE IF NOT EXISTS `task_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `mentioned_user_ids` JSON COMMENT '@提及的用户ID列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 14. 竞赛信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `competition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `level` VARCHAR(20),
    `category` VARCHAR(30),
    `register_start` DATE,
    `register_end` DATE,
    `event_start` DATE,
    `event_end` DATE,
    `organizer` VARCHAR(200),
    `official_url` VARCHAR(500),
    `description` TEXT,
    `schedule` JSON,
    `view_count` INT DEFAULT 0,
    `collect_count` INT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'UPCOMING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 15. 导师表
-- =============================================
CREATE TABLE IF NOT EXISTS `teacher` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `department` VARCHAR(100),
    `title` VARCHAR(50),
    `research_fields` JSON,
    `max_teams` INT DEFAULT 3,
    `current_teams` INT DEFAULT 0,
    `is_verified` TINYINT DEFAULT 0,
    `rating` DECIMAL(2,1) DEFAULT 0,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 16. 成果表
-- =============================================
CREATE TABLE IF NOT EXISTS `achievement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` BIGINT,
    `team_id` BIGINT,
    `submitter_id` BIGINT,
    `title` VARCHAR(200) NOT NULL,
    `type` VARCHAR(30),
    `description` TEXT,
    `award_level` VARCHAR(50),
    `file_urls` JSON,
    `is_public` TINYINT DEFAULT 1,
    `is_featured` TINYINT DEFAULT 0,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`project_id`) REFERENCES `project`(`id`),
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 17. 文件表
-- =============================================
CREATE TABLE IF NOT EXISTS `file` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT,
    `project_id` BIGINT COMMENT '关联项目ID',
    `task_id` BIGINT COMMENT '关联任务ID，上传时可为空，创建任务后关联',
    `parent_id` BIGINT,
    `name` VARCHAR(255) NOT NULL,
    `type` VARCHAR(10) DEFAULT 'FILE',
    `file_key` VARCHAR(255),
    `file_size` BIGINT,
    `mime_type` VARCHAR(100),
    `version` INT DEFAULT 1,
    `uploader_id` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`),
    FOREIGN KEY (`uploader_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 18. 即时消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT,
    `content_type` VARCHAR(10) DEFAULT 'TEXT',
    `file_url` VARCHAR(500),
    `is_read` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`),
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 19. 通知表
-- =============================================
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(30),
    `is_read` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 20. 日历事件表
-- =============================================
CREATE TABLE IF NOT EXISTS `calendar_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `type` VARCHAR(30),
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME,
    `description` VARCHAR(500),
    `remind_time` DATETIME,
    `source_id` BIGINT,
    `is_reminded` TINYINT DEFAULT 0,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 21. 审核日志表
-- =============================================
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type` VARCHAR(30),
    `target_id` BIGINT,
    `operator_id` BIGINT,
    `action` VARCHAR(20),
    `reason` VARCHAR(500),
    `before_status` VARCHAR(20),
    `after_status` VARCHAR(20),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`operator_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 22. 公告表
-- =============================================
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(30) DEFAULT 'SYSTEM',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 23. 系统配置表
-- =============================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) UNIQUE NOT NULL,
    `config_value` TEXT,
    `description` VARCHAR(500),
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 24. 徽章定义表
-- =============================================
CREATE TABLE IF NOT EXISTS `badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(200),
    `icon` VARCHAR(50) DEFAULT 'badge_default',
    `condition_type` VARCHAR(30) COMMENT 'COMPLETED_TASKS / TEAM_COUNT / SKILL_COUNT / ACHIEVEMENT_COUNT / COMPETITION_COUNT / MASTER_SKILL / HAS_HONOR / RECRUIT_COUNT / PROJECT_COUNT / EXPERIENCE_COUNT / ALL_ROUNDER',
    `condition_value` INT DEFAULT 0,
    `category` VARCHAR(30) DEFAULT 'general',
    `sort_order` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 25. 奖状/证书表
-- =============================================
CREATE TABLE IF NOT EXISTS `certificate` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '获奖用户ID',
    `badge_id` BIGINT NOT NULL COMMENT '对应徽章ID',
    `badge_name` VARCHAR(50) NOT NULL COMMENT '徽章/奖项名称',
    `user_name` VARCHAR(50) NOT NULL COMMENT '用户姓名（生成时快照）',
    `student_id` VARCHAR(20) COMMENT '学号（生成时快照）',
    `image_path` VARCHAR(500) COMMENT '奖状图片路径',
    `awarded_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获奖时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 26. AI 聊天会话表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) DEFAULT '新的对话' COMMENT '会话标题（由AI自动生成）',
    `summary` TEXT COMMENT '历史对话摘要（压缩后的旧消息）',
    `message_count` INT DEFAULT 0 COMMENT '消息总数',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 27. AI 聊天消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（冗余，方便按用户查询）',
    `role` VARCHAR(20) NOT NULL COMMENT 'user / assistant / system',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `compressed` TINYINT DEFAULT 0 COMMENT '是否已被压缩（1=已压缩到摘要中）',
    `token_count` INT DEFAULT 0 COMMENT '估算token数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 初始化示例数据
-- 
-- 注意：用户、技能、招募、团队、项目、通知、导师、成果、审核日志、竞赛
-- 等数据已由 DataInitializer.java 统一管理（启动时自动初始化），
-- 以下仅包含 SQL 独有的初始化数据。
-- 所有 INSERT 使用 WHERE NOT EXISTS 保证幂等性，可重复执行。
-- =============================================

-- 系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
SELECT * FROM (SELECT 'platform.name' AS k, '大学生竞赛/科研项目组队匹配与过程管理平台' AS v, '平台名称' AS d) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = tmp.k) LIMIT 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
SELECT * FROM (SELECT 'platform.version', '1.0.0', '平台版本号') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'platform.version') LIMIT 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
SELECT * FROM (SELECT 'audit.required', 'true', '是否需要审核') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'audit.required') LIMIT 1;

-- 任务执行人关联
INSERT IGNORE INTO `task_assignee` (`task_id`, `user_id`, `assignment_note`, `skill_match_score`, `status`) VALUES
(1, 2, '整体负责需求分析和系统设计', 0.95, 'ACCEPTED'),
(2, 4, '负责用户管理模块的后端开发', 0.88, 'ACCEPTED'),
(3, 6, '负责数据采集和存储模块', 0.82, 'ASSIGNED'),
(4, 5, '负责核心推荐算法实现', 0.91, 'ASSIGNED'),
(5, 4, '负责前端页面开发', 0.75, 'ASSIGNED');

-- 任务成果提交
INSERT IGNORE INTO `task_submission` (`task_id`, `submitter_id`, `title`, `description`, `status`, `review_comment`, `reviewer_id`, `reviewed_at`)
VALUES (1, 2, '需求分析文档V1.0', '包含功能需求、非功能需求、用例图和系统架构图，共计45页。', 'APPROVED', '文档内容详尽，架构设计合理，通过验收。', 2, '2026-06-14 10:30:00');

INSERT IGNORE INTO `task_submission` (`task_id`, `submitter_id`, `title`, `description`, `status`)
VALUES (2, 4, '用户注册登录模块初版', '已完成用户注册、登录、JWT鉴权功能的后端API开发，正在进行前端联调。', 'PENDING_REVIEW');

-- 任务评论
INSERT INTO `task_comment` (`task_id`, `user_id`, `content`) VALUES
(1, 2, '需求分析文档已经完成，请大家查阅并提出修改意见。'),
(1, 4, '收到，架构设计清晰，数据库设计也符合预期。'),
(2, 2, '用户管理模块的API文档已经更新到Swagger，注意接口鉴权逻辑。');
INSERT INTO `task_comment` (`task_id`, `user_id`, `content`, `mentioned_user_ids`) VALUES
(2, 4, '王五，注册接口的验证码逻辑需要增加限流保护。', '[4]');

-- 公告数据
INSERT IGNORE INTO `announcement` (`title`, `content`, `type`) VALUES
('平台正式上线公告', '大学生竞赛/科研项目组队匹配与过程管理平台已正式上线！欢迎大家注册使用，发布招募信息，组建参赛团队。', 'SYSTEM'),
('2026年互联网+大赛报名通知', '2026年中国国际"互联网+"大学生创新创业大赛已启动报名，报名截止时间：2026年5月30日。请有意参赛的同学尽快组队并提交报名材料。', 'COMPETITION'),
('团队协作功能更新', '平台新增任务中心功能，支持多人任务指派、成果提交和评论沟通，帮助团队更高效地协作完成项目。', 'SYSTEM');

-- =============================================
-- 初始化完成
-- =============================================
SELECT '✅ 数据库初始化完成！共创建 27 张表' AS 状态;
