-- =============================================
-- 任务中心功能升级 - 数据库迁移脚本
-- 执行前请备份数据库！
-- 支持：多人指派、成果提交、评论沟通
-- =============================================

-- 1. 项目表添加 leader_id 字段（先允许NULL，填数据后再加NOT NULL和外键）
ALTER TABLE `project`
    ADD COLUMN `leader_id` BIGINT COMMENT '项目负责人' AFTER `team_id`;

-- 1.1 将已有项目的 leader_id 设置为该团队的第一个成员或队长
UPDATE `project` p
SET `leader_id` = (
    SELECT COALESCE(
        (SELECT `user_id` FROM `team_member` WHERE `team_id` = p.`team_id` AND `role` = 'LEADER' LIMIT 1),
        (SELECT `user_id` FROM `team_member` WHERE `team_id` = p.`team_id` LIMIT 1),
        (SELECT `id` FROM `user` LIMIT 1)
    )
)
WHERE `leader_id` IS NULL;

-- 1.2 改为 NOT NULL 并添加外键
ALTER TABLE `project`
    MODIFY COLUMN `leader_id` BIGINT NOT NULL COMMENT '项目负责人',
    ADD FOREIGN KEY `fk_project_leader` (`leader_id`) REFERENCES `user`(`id`);

-- 2. 修改 task 表结构（先处理旧外键）
-- 2.1 删除旧的 assignee_id 外键约束（外键名可能不同，尝试常见的几种）
-- 先查一下实际外键名：SHOW CREATE TABLE task;
SET @fk_name = (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task'
    AND COLUMN_NAME = 'assignee_id' AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_fk_sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `task` DROP FOREIGN KEY `', @fk_name, '`'),
    'SELECT "No assignee_id FK found, skip" AS info'
);
PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.2 删除 assignee_id 列
ALTER TABLE `task`
    DROP COLUMN IF EXISTS `assignee_id`;

-- 2.3 添加新字段（先允许NULL，填数据后再约束）
ALTER TABLE `task`
    ADD COLUMN IF NOT EXISTS `creator_id` BIGINT COMMENT '任务创建者（项目负责人）' AFTER `project_id`,
    ADD COLUMN IF NOT EXISTS `required_skills` VARCHAR(500) COMMENT '任务所需技能，逗号分隔' AFTER `description`,
    ADD COLUMN IF NOT EXISTS `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- 2.4 为已有任务设置 creator_id（取项目负责人，没有则取第一个用户）
UPDATE `task` t
LEFT JOIN `project` p ON t.`project_id` = p.`id`
SET t.`creator_id` = p.`leader_id`
WHERE t.`creator_id` IS NULL;

UPDATE `task` t
SET t.`creator_id` = (SELECT `id` FROM `user` LIMIT 1)
WHERE t.`creator_id` IS NULL;

-- 2.5 修改 creator_id 为 NOT NULL 并添加外键
ALTER TABLE `task`
    MODIFY COLUMN `creator_id` BIGINT NOT NULL COMMENT '任务创建者（项目负责人）',
    MODIFY COLUMN `status` VARCHAR(20) DEFAULT 'TODO' COMMENT 'TODO/IN_PROGRESS/SUBMITTED/REVIEWING/DONE/OVERDUE',
    ADD FOREIGN KEY `fk_task_creator` (`creator_id`) REFERENCES `user`(`id`);

-- 3. 创建任务执行人关联表
CREATE TABLE IF NOT EXISTS `task_assignee` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '执行人ID',
    `assignment_note` VARCHAR(500) COMMENT '负责人对该执行人的分工说明',
    `skill_match_score` DECIMAL(3,2) DEFAULT 0 COMMENT '技能匹配度（0-1）',
    `status` VARCHAR(20) DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED/ACCEPTED/REJECTED',
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '指派时间',
    `accepted_at` DATETIME COMMENT '接受时间',
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_task_user` (`task_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行人关联表';

-- 4. 创建任务成果提交表
CREATE TABLE IF NOT EXISTS `task_submission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '成果标题',
    `description` TEXT COMMENT '成果描述',
    `file_urls` JSON COMMENT '附件URL列表',
    `status` VARCHAR(20) DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/APPROVED/REJECTED',
    `review_comment` TEXT COMMENT '验收意见',
    `reviewer_id` BIGINT COMMENT '验收人ID',
    `reviewed_at` DATETIME COMMENT '验收时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务成果提交表';

-- 5. 创建任务评论表
CREATE TABLE IF NOT EXISTS `task_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '评论人ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `mentioned_user_ids` JSON COMMENT '@提及的用户ID列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务评论表';

-- =============================================
-- 迁移完成后执行验证查询
-- =============================================
SELECT '✅ project表leader_id字段' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME='leader_id';
SELECT '✅ task_assignee表' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='task_assignee';
SELECT '✅ task_submission表' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='task_submission';
SELECT '✅ task_comment表' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='task_comment';
