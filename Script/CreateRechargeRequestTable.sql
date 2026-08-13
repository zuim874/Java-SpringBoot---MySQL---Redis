-- =============================================
-- 数据库名称：springbootdb
-- 说明：用户充值申请记录表（管理员审核制）
-- 创建时间：2026-08-13
-- =============================================

USE springbootdb;

-- =============================================
-- 充值申请表（对应实体类 RechargeRequest）
-- 1.用户提交充值申请，status=0 为待审核
-- 2.管理员审核通过(status=1)后自动增加余额
-- 3.审核不通过(status=2)则拒绝
-- =============================================
DROP TABLE IF EXISTS sys_recharge_request;
CREATE TABLE sys_recharge_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联 sys_user.id',
    username VARCHAR(50) NOT NULL COMMENT '用户名（冗余，方便管理员查看）',
    amount DECIMAL(10,2) NOT NULL COMMENT '申请充值金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注/拒绝原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '审核时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户充值申请表';