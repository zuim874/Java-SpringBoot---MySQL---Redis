-- =============================================
-- 数据库名称：springbootdb
-- 说明：会员体系扩展脚本（优惠券 / 用户优惠券 / 买卖家会话 / 聊天消息）
-- 创建时间：2026-08-13
-- 前置：需先执行 finalInitSql/InitDataBase.sql（已包含 sys_user / sys_seller / sys_product / sys_role）
-- 角色约定（sys_user.user_role 逗号分隔多角色）：
--   ROLE_USER        普通买家
--   ROLE_VIP_USER    会员买家（享受更多/更强优惠券）
--   ROLE_SELLER      普通卖家
--   ROLE_VIP_SELLER  会员卖家（享受推荐位/旗舰店标识等福利）
-- =============================================

USE springbootdb;

-- =============================================
-- 1. 优惠券模板表（管理员创建，用于发放给买家）
-- =============================================
DROP TABLE IF EXISTS sys_coupon;
CREATE TABLE sys_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称（如：新客满减券 / 会员尊享券）',
    type TINYINT NOT NULL COMMENT '优惠类型：1满减 2折扣',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠值：满减为减免金额(元)，折扣为折数(如8.00表示8折)',
    min_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛：订单满X元可用',
    total_count INT NOT NULL DEFAULT 0 COMMENT '发行总量',
    remain_count INT NOT NULL DEFAULT 0 COMMENT '剩余可发放数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
    target_type TINYINT NOT NULL DEFAULT 1 COMMENT '适用人群：1全部用户（普通券） 2仅VIP（VIP券，受众=VIP用户+VIP卖家）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- =============================================
-- 2. 用户优惠券表（已发放到买家账户的优惠券）
-- 快照冗余 name/type/discount_value/min_amount，避免模板后续变更影响已发放的券
-- =============================================
DROP TABLE IF EXISTS sys_user_coupon;
CREATE TABLE sys_user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '持有者用户ID，关联 sys_user.id',
    coupon_id BIGINT NOT NULL COMMENT '来源优惠券模板ID，关联 sys_coupon.id',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称（快照）',
    type TINYINT NOT NULL COMMENT '优惠类型（快照）：1满减 2折扣',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠值（快照）',
    min_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛（快照）',
    target_type TINYINT NOT NULL DEFAULT 1 COMMENT '适用人群快照：1普通 2VIP专属',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用 2已过期',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    receive_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time DATETIME DEFAULT NULL COMMENT '使用时间',
    order_id BIGINT DEFAULT NULL COMMENT '使用的订单ID（status=1时有效）',
    INDEX idx_user_status (user_id, status) COMMENT '用户+状态索引（高频：我的优惠券）',
    INDEX idx_expire (expire_time) COMMENT '过期时间索引（定时清理）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- =============================================
-- 3. 会话表（买卖双方沟通的会话，一对一）
-- =============================================
DROP TABLE IF EXISTS sys_conversation;
CREATE TABLE sys_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '买家用户ID，关联 sys_user.id',
    seller_id BIGINT NOT NULL COMMENT '卖家ID，关联 sys_seller.id',
    last_message VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容（列表预览）',
    unread_user INT NOT NULL DEFAULT 0 COMMENT '买家未读数',
    unread_seller INT NOT NULL DEFAULT 0 COMMENT '卖家未读数',
    last_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后消息时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_seller (user_id, seller_id) COMMENT '唯一约束：买卖双方只允许一个会话'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='买卖会话表';

-- =============================================
-- 4. 聊天消息表（会话下的所有消息）
-- =============================================
DROP TABLE IF EXISTS sys_chat_message;
CREATE TABLE sys_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID，关联 sys_conversation.id',
    sender_id BIGINT NOT NULL COMMENT '发送者ID（用户ID或卖家ID）',
    sender_role VARCHAR(20) NOT NULL COMMENT '发送者身份：USER买家 / SELLER卖家',
    content VARCHAR(500) NOT NULL COMMENT '消息内容',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_conversation_time (conversation_id, create_time) COMMENT '会话+时间索引（拉取消息）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- =============================================
-- 5. 会员卖家福利扩展：商品表新增「推荐位」标记
-- （VIP卖家可将商品置为推荐，商城首页置顶展示）
-- =============================================
ALTER TABLE sys_product
    ADD COLUMN recommend TINYINT NOT NULL DEFAULT 0 COMMENT '推荐位：0普通 1推荐（VIP卖家权益）' AFTER main_image_url,
    ADD INDEX idx_recommend (recommend) COMMENT '推荐位索引';

-- =============================================
-- 6. 初始化优惠券模板示例数据
-- 普通买家券：力度较小；会员买家券：力度更大
-- =============================================
INSERT IGNORE INTO sys_coupon (id, name, type, discount_value, min_amount, total_count, remain_count, status, target_type) VALUES
(1, '新客满减券',            1, 5.00,  50,   10000, 10000, 1, 1),
(2, '普通用户折扣券',        2, 9.50,  100,  10000, 10000, 1, 1),
(3, '会员尊享满减券',        1, 30.00, 200,  5000,  5000,  1, 2),
(4, '会员尊享折扣券',        2, 8.50,  300,  5000,  5000,  1, 2);
