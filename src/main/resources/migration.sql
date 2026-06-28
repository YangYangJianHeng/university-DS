-- 为 file 表添加 task_id 字段（已有表升级用）
-- 注意：IF NOT EXISTS 在部分MySQL版本不支持，如果已存在列会报错但可忽略
ALTER TABLE `file` ADD COLUMN `task_id` BIGINT COMMENT '关联任务ID' AFTER `project_id`;
