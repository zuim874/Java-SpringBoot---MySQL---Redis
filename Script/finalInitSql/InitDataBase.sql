-- =============================================
-- 数据库名称：springbootdb
-- 字符集：UTF-8MB4（支持emoji和特殊字符）
-- 说明：SpringBoot项目【建库建表】脚本（仅 DDL，不含任何数据插入）
-- 已合并：订单表 / 操作日志表 / 收货地址表 / 充值申请表
--         优惠券表 / 用户优惠券表 / 买卖会话表 / 聊天消息表
--         商品分类表 sys_category（管理员维护）
-- 注意：
--   1. 本脚本只包含表结构定义，所有初始化数据（角色/卖家/商品/图片/优惠券/分类）
--      请执行同目录下的 InitRealDataBase.sql
--   2. 商品表已内置推荐位 recommend 字段（会员卖家权益）
--   3. 本脚本不包含任何用户账号；管理员与卖家登录账号由
--      项目启动时的 InitAdminRunner / InitSellerAccountsRunner 自动创建
--      （管理员 admin/123456；卖家账号=店铺名称/默认密码 Seller@123）
-- 用法：新环境依次执行本文件（建库建表）→ InitRealDataBase.sql（初始化数据）
-- 注意：执行会先 DROP 旧库，请确认操作环境
-- =============================================

-- 1. 删除旧数据库（如果存在，慎重！）
DROP DATABASE IF EXISTS springbootdb;

-- 2. 创建新数据库
CREATE DATABASE IF NOT EXISTS springbootdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 3. 切换到该数据库
USE springbootdb;

-- =============================================
-- 第一部分：核心权限表（用户、角色）
-- =============================================

-- 3.1 用户表（对应实体类 User）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一）',
                          password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
                          nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                          user_role VARCHAR(50) DEFAULT 'ROLE_USER' COMMENT '用户权限',
                          avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                          email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                          status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
                          balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（元，充值/支付用）',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
                          is_deleted TINYINT DEFAULT 0 COMMENT '状态：0未删除 1删除',
                          INDEX idx_username (username) COMMENT '用户名索引（高频查询）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3.2 角色表（RBAC权限模型）
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                          role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码（如：ROLE_ADMIN）',
                          role_name VARCHAR(50) NOT NULL COMMENT '角色名称（如：管理员）',
                          description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
                          status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          INDEX idx_role_code (role_code) COMMENT '角色编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 第二部分：商品体系表（分类、商品、卖家、商品图片）
-- =============================================

-- 3.3 商品分类表（对应实体类 Category，仅管理员维护）
-- 说明：分类由管理员预设，卖家新增/编辑商品时从本表拉取并多选
DROP TABLE IF EXISTS sys_category;
CREATE TABLE sys_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 3.4 商品表
DROP TABLE IF EXISTS sys_product;
CREATE TABLE sys_product (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                             seller_id BIGINT NOT NULL COMMENT '卖家ID，关联sys_seller.id',
                             product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
                             price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品价格(元)',
                             stock INT NOT NULL DEFAULT 0 COMMENT '商品库存数量',
                             sold INT NOT NULL DEFAULT 0 COMMENT '已售出数量',
                             status TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态：0下架 1上架',
                             description VARCHAR(500) NOT NULL COMMENT '商品描述',
                             category VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品分类ID集合，英文逗号分隔（支持多分类，如：1,2）',
                             main_image_url VARCHAR(255) NULL COMMENT '冗余：商品主图URL，列表页查询优化',
                             recommend TINYINT NOT NULL DEFAULT 0 COMMENT '推荐位：0普通 1推荐（会员卖家权益，商城置顶曝光）',
                             is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                             create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                             INDEX idx_seller_id (seller_id),
                             INDEX idx_product_name (product_name),
                             INDEX idx_category (category),
                             INDEX idx_recommend (recommend) COMMENT '推荐位索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 3.4 卖家表
DROP TABLE IF EXISTS sys_seller;
CREATE TABLE sys_seller (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                            seller_name VARCHAR(50) NOT NULL COMMENT '卖家名称',
                            seller_avatar VARCHAR(255) NULL COMMENT '卖家头像URL（店铺Logo）',
                            seller_contact VARCHAR(100) NULL COMMENT '卖家联系方式（客服电话/邮箱）',
                            address VARCHAR(255) NOT NULL COMMENT '卖家地址',
                            is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                            create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                            update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                            INDEX idx_seller_name (seller_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卖家信息表';

-- 3.5 商品图片表（一对多）
DROP TABLE IF EXISTS product_image;
CREATE TABLE product_image (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               product_id BIGINT NOT NULL COMMENT '关联商品id',
                               image_url VARCHAR(255) NOT NULL COMMENT '图片访问地址',
                               sort INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
                               is_main TINYINT NOT NULL DEFAULT 0 COMMENT '0普通图 1主图',
                               is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                               create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                               INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- =============================================
-- 第三部分：订单体系表（订单、订单项）
-- =============================================

-- 3.6 订单表（对应实体类 Order）
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

-- 3.7 订单项表（对应实体类 OrderItem）
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

-- =============================================
-- 第四部分：业务扩展表（操作日志、收货地址、充值申请）
-- =============================================

-- 3.8 用户操作日志表（对应实体类 OperationLog，配合 ActiveMQ 异步记录）
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

-- 3.9 用户收货地址表（对应实体类 UserAddress，支持预设地址选择）
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

-- 3.10 用户充值申请表（对应实体类 RechargeRequest，管理员审核制）
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

-- 3.11 优惠券模板表（对应实体类 Coupon，管理员创建，用于发放给买家）
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

-- 3.12 用户优惠券表（对应实体类 UserCoupon，已发放到买家账户的券，含快照信息）
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

-- 3.13 买卖会话表（对应实体类 Conversation，买卖双方一对一沟通）
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

-- 3.14 聊天消息表（对应实体类 ChatMessage，会话下的所有消息）
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
-- 第五部分：初始数据（已拆分）
-- 说明：所有初始化数据（角色/卖家/商品分类/商品/商品图片/优惠券）
--       已迁移至同目录下 InitRealDataBase.sql，请另行执行
-- =============================================

-- 5.3 插入商品数据（已迁移至 InitRealDataBase.sql）

-- 5.4 插入商品图片（已迁移至 InitRealDataBase.sql）

-- 5.5 插入优惠券模板示例数据（已迁移至 InitRealDataBase.sql）
