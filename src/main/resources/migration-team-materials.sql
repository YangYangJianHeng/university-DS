-- =============================================
-- 团队功能升级 - 数据库迁移脚本
-- 1. team_teacher 表添加邀请消息字段
-- 2. team 表添加描述字段
-- 执行前请备份数据库！
-- =============================================

-- 1. team_teacher 表添加 message 列（邀请导师时的留言）
ALTER TABLE `team_teacher`
    ADD COLUMN IF NOT EXISTS `message` VARCHAR(500) COMMENT '邀请留言信息' AFTER `status`;

-- 2. team 表添加 description 列（团队描述/简介）
ALTER TABLE `team`
    ADD COLUMN IF NOT EXISTS `description` TEXT COMMENT '团队描述' AFTER `name`;

-- =============================================
-- 迁移验证
-- =============================================
SELECT '✅ team_teacher.message' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='team_teacher' AND COLUMN_NAME='message';
SELECT '✅ team.description' AS 检查项, COUNT(*) AS 记录数 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='team' AND COLUMN_NAME='description';
