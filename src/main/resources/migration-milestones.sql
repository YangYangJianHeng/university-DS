-- =============================================
-- 迁移：为project表添加milestones字段
-- 将里程碑数据从description字段独立出来
-- =============================================
ALTER TABLE `project`
    ADD COLUMN `milestones` TEXT COMMENT '里程碑JSON数组' AFTER `description`;
