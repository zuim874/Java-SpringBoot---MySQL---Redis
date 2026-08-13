-- =============================================
-- 数据库名称：springbootdb
-- 说明：用户收货地址表（支持预设地址选择）
-- 创建时间：2026-08-13
-- =============================================

USE springbootdb;

-- =============================================
-- 用户收货地址表（对应实体类 UserAddress）
-- 1.每个用户可维护多个收货地址
-- 2.is_default 标记默认地址，下单时自动选中
-- 3.逻辑删除字段 is_deleted
-- =============================================
DROP TABLE IF EXISTS sys_user_address;
CREATE TABLE sys_user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联 sys_user.id',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    receiver_address VARCHAR(255) NOT NULL COMMENT '收货地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0否 1是',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引（高频查询）',
    INDEX idx_is_default (user_id, is_default) COMMENT '默认地址索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';