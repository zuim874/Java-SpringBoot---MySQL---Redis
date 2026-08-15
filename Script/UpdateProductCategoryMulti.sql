-- =============================================
-- 商品分类扩展为多分类支持
-- 说明：商品管理支持"一个商品分配多种分类"，
--       sys_product.category 字段以英文逗号分隔存储多个分类，
--       例如 '手机配件,电脑外设'，前端查询通过 FIND_IN_SET 匹配。
--       原字段长度 VARCHAR(50) 偏短，扩展为 VARCHAR(255) 防止溢出。
-- 用法：在 MySQL 中执行本脚本即可（可重复执行，结果幂等）
-- =============================================

ALTER TABLE sys_product
    MODIFY COLUMN category VARCHAR(255) NOT NULL DEFAULT '其他' COMMENT '商品分类，支持多分类以英文逗号分隔（如：手机配件,电脑外设）';

-- 校验：确认修改生效
SHOW COLUMNS FROM sys_product LIKE 'category';
