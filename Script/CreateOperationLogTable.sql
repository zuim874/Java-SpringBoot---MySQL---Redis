-- =============================================
-- 数据库名称：admin_db（springbootdb）
-- 说明：用户操作日志表（配合 ActiveMQ 异步记录）
-- 创建时间：2026-08-12
-- =============================================

USE springbootdb;

-- =============================================
-- 操作日志表（对应实体类 OperationLog）
-- 1.记录用户的关键操作行为，如登录、注册、修改密码、充值、下单等
-- 2.通过 ActiveMQ 异步写入，不阻塞主业务流程
-- 3.支持按用户、操作类型、时间范围等维度查询
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID（可能为null，如未登录）',
    username VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作名称（如：用户登录、用户注册、修改密码、用户充值）',
    method VARCHAR(255) DEFAULT NULL COMMENT '请求方法（Controller类名.方法名）',
    request_params TEXT DEFAULT NULL COMMENT '请求参数（JSON格式）',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    request_ip VARCHAR(50) DEFAULT NULL COMMENT '请求IP地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0失败 1成功',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息（失败时记录）',
    duration BIGINT DEFAULT NULL COMMENT '请求耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_username (username) COMMENT '用户名索引',
    INDEX idx_operation (operation) COMMENT '操作名称索引',
    INDEX idx_create_time (create_time) COMMENT '时间索引（便于按时间范围查询）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作日志表';