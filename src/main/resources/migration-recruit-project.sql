-- =============================================
-- 迁移脚本：招募转项目功能
-- 为recruit表添加project_id、team_id、continue_recruit字段
-- =============================================

-- MySQL版本
ALTER TABLE `recruit`
    ADD COLUMN IF NOT EXISTS `project_id` BIGINT COMMENT '关联的项目ID（转为项目后填充）',
    ADD COLUMN IF NOT EXISTS `team_id` BIGINT COMMENT '关联的团队ID（转为项目后填充）',
    ADD COLUMN IF NOT EXISTS `continue_recruit` TINYINT DEFAULT 0 COMMENT '转为项目后是否继续招募：0=停止，1=继续';

-- 添加外键（如果列已存在）
-- ALTER TABLE `recruit` ADD FOREIGN KEY (`project_id`) REFERENCES `project`(`id`);
-- ALTER TABLE `recruit` ADD FOREIGN KEY (`team_id`) REFERENCES `team`(`id`);
