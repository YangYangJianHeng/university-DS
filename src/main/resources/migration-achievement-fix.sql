-- =============================================
-- Achievement 表结构修复迁移脚本
-- 用途：修复旧版init-all.sql创建的表缺少submitter_id字段的问题
-- 使用方式：在MySQL中手动执行此脚本
-- 注意：如果字段已存在会报错（可忽略），不影响数据
-- =============================================

-- 添加 submitter_id 字段
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'achievement'
       AND COLUMN_NAME = 'submitter_id') > 0,
    'SELECT ''COLUMN_EXISTS''',
    'ALTER TABLE achievement ADD COLUMN submitter_id BIGINT AFTER team_id'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
