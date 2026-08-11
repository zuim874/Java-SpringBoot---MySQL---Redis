-- =============================================
-- 订单系统数据表创建脚本
-- 说明：在原有数据库基础上新增订单相关表
-- =============================================

-- 切换到该数据库
USE springbootdb;

-- =============================================
-- 1. 订单表（对应实体类 Order）
-- =============================================
DROP TABLE IF EXISTS sys_order;
CREATE TABLE sys_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联sys_user.id',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号（唯一，由OrderNoGenerator生成）',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额(元)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付 1已支付 2已发货 3已完成 4已取消 5已退款',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式（如：微信支付、支付宝）',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    receiver_address VARCHAR(255) NOT NULL COMMENT '收货地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    pay_time DATETIME(3) DEFAULT NULL COMMENT '支付时间',
    ship_time DATETIME(3) DEFAULT NULL COMMENT '发货时间',
    complete_time DATETIME(3) DEFAULT NULL COMMENT '完成时间',
    cancel_time DATETIME(3) DEFAULT NULL COMMENT '取消时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引（高频查询：用户订单列表）',
    INDEX idx_order_no (order_no) COMMENT '订单号索引',
    INDEX idx_status (status) COMMENT '订单状态索引（管理员筛选）',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引（排序）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =============================================
-- 2. 订单项表（对应实体类 OrderItem）
-- =============================================
DROP TABLE IF EXISTS sys_order_item;
CREATE TABLE sys_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID，关联sys_order.id',
    product_id BIGINT NOT NULL COMMENT '商品ID，关联sys_product.id',
    product_name VARCHAR(100) NOT NULL COMMENT '下单时商品名称（快照）',
    product_image VARCHAR(255) DEFAULT NULL COMMENT '下单时商品主图URL（快照）',
    price DECIMAL(10,2) NOT NULL COMMENT '下单时商品单价（快照）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额（price * quantity）',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    INDEX idx_order_id (order_id) COMMENT '订单ID索引（高频查询：订单项列表）',
    INDEX idx_product_id (product_id) COMMENT '商品ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';