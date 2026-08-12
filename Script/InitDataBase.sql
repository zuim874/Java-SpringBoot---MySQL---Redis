-- =============================================
-- 数据库名称：admin_db
-- 字符集：UTF-8MB4（支持emoji和特殊字符）
-- 说明：SpringBoot项目完整数据表初始化脚本
-- =============================================

-- 1. 删除旧数据库（如果存在，慎重！）
DROP DATABASE IF EXISTS admin_db;

-- 2. 创建新数据库
CREATE DATABASE IF NOT EXISTS springbootdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 3. 切换到该数据库
USE springbootdb;

-- =============================================
-- 第一部分：核心权限表（用户、角色、菜单）
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
                          balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（元，充值/支付用）',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          INDEX idx_role_code (role_code) COMMENT '角色编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 3.3 商品表
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
                             category VARCHAR(50) NOT NULL DEFAULT '其他' COMMENT '商品分类（手机配件/电脑外设/音频设备/智能家居/穿戴设备/摄影器材/其他）',
                             main_image_url VARCHAR(255) NULL COMMENT '冗余：商品主图URL，列表页查询优化',
                             is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                             create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                             INDEX idx_seller_id (seller_id),
                             INDEX idx_product_name (product_name),
                             INDEX idx_category (category)
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

-- 5.1 插入管理员角色
INSERT INTO sys_role (role_code, role_name, description, status) VALUES
                                                                     ('ROLE_ADMIN', '超级管理员', '拥有所有权限', 1),
                                                                     ('ROLE_USER', '普通用户', '基础权限', 1),
                                                                     ('ROLE_VIP_USER', '会员用户', '会员用户权限', 1),
                                                                     ('ROLE_SELLER', '普通卖家', '普通卖家权限', 1),
                                                                     ('ROLE_VIP_SELLER', '会员卖家', '会员卖家权限', 1);

-- =============================================
-- 第二部分：商品初始数据
-- =============================================

-- 6.1 插入卖家数据
INSERT INTO sys_seller (seller_name, seller_avatar, seller_contact, address) VALUES
                                                  ('ZuiM官方旗舰店', '/uploads/sellers/default.png', '400-888-0001', '广东省深圳市南山区科技园'),
                                                  ('数码优选专营店', '/uploads/sellers/default.png', '400-888-0002', '北京市海淀区中关村大街'),
                                                  ('潮玩科技馆', '/uploads/sellers/default.png', '400-888-0003', '上海市浦东新区张江高科技园区'),
                                                  ('品质生活家居馆', '/uploads/sellers/default.png', '400-888-0004', '浙江省杭州市余杭区未来科技城'),
                                                  ('摄影器材总汇', '/uploads/sellers/default.png', '400-888-0005', '广东省广州市天河区珠江新城');

-- 6.2 插入商品数据（12个商品覆盖6个分类）
INSERT INTO sys_product (seller_id, product_name, price, stock, sold, status, description, category, main_image_url) VALUES
-- 手机配件（seller_id=1）
(1, '极速无线充电器', 129.00, 200, 156, 1, '15W 快充 · 兼容 iPhone/Android · 轻薄便携', '手机配件', '/uploads/products/product-1.jpg'),
(1, '防摔手机壳 磁吸系列', 49.00, 500, 380, 1, '军工级防摔 · 磁吸兼容 · 多色可选 · 手感细腻', '手机配件', '/uploads/products/product-2.jpg'),
-- 电脑外设（seller_id=2）
(2, '机械键盘 K8 Pro', 399.00, 100, 78, 1, '87键 · 青轴 · RGB背光 · 铝合金机身', '电脑外设', '/uploads/products/product-3.jpg'),
(2, 'USB-C 扩展坞 HubMax', 259.00, 150, 112, 1, '12合1 · 4K60Hz · 100W PD · 千兆网口', '电脑外设', '/uploads/products/product-4.jpg'),
-- 音频设备（seller_id=3）
(3, '降噪耳机 AirSound', 599.00, 80, 65, 1, '主动降噪 · 40h续航 · 蓝牙5.3 · Hi-Res认证', '音频设备', '/uploads/products/product-5.jpg'),
(3, '蓝牙音箱 MiniBeat', 179.00, 120, 89, 1, '360°环绕声 · 12h续航 · IPX5防水 · 小巧便携', '音频设备', '/uploads/products/product-6.jpg'),
-- 智能家居（seller_id=4）
(4, '智能台灯 Lumina', 249.00, 90, 67, 1, '无级调光 · 色温调节 · 护眼模式 · 智能联动', '智能家居', '/uploads/products/product-7.jpg'),
(4, '智能插座 SmartPlug', 89.00, 300, 220, 1, '远程控制 · 电量统计 · 语音控制 · 定时开关', '智能家居', '/uploads/products/product-8.jpg'),
-- 穿戴设备（seller_id=5）
(5, '运动手环 FitBand', 199.00, 180, 145, 1, '心率监测 · 睡眠分析 · IP68防水 · 14天续航', '穿戴设备', '/uploads/products/product-9.jpg'),
(5, '智能手表 WatchX', 899.00, 60, 42, 1, 'AMOLED屏 · eSIM · 7天续航 · 血氧监测', '穿戴设备', '/uploads/products/product-10.jpg'),
-- 摄影器材（seller_id=1）
(1, '便携三脚架 ProPod', 159.00, 75, 58, 1, '碳纤维 · 1.5kg承重 · 折叠便携 · 快装板', '摄影器材', '/uploads/products/product-11.jpg'),
-- 电脑外设（seller_id=2）
(2, '显示器支架 ArmOne', 299.00, 65, 48, 1, '气动悬臂 · 17-32寸 · 理线设计 · 桌面夹式', '电脑外设', '/uploads/products/product-12.jpg');

-- 6.3 插入商品图片（每件商品2-3张轮播图，含主图标记）
INSERT INTO product_image (product_id, image_url, sort, is_main) VALUES
-- 商品1：极速无线充电器
(1, '/uploads/products/product-1.jpg', 0, 1),
(1, '/uploads/products/product-1-2.jpg', 1, 0),
(1, '/uploads/products/product-1.jpg', 2, 0),
-- 商品2：防摔手机壳
(2, '/uploads/products/product-2.jpg', 0, 1),
(2, '/uploads/products/product-2.jpg', 1, 0),
-- 商品3：机械键盘
(3, '/uploads/products/product-3.jpg', 0, 1),
(3, '/uploads/products/product-3.jpg', 1, 0),
-- 商品4：扩展坞
(4, '/uploads/products/product-4.jpg', 0, 1),
-- 商品5：降噪耳机
(5, '/uploads/products/product-5.jpg', 0, 1),
(5, '/uploads/products/product-1.jpg', 1, 0),
-- 商品6：蓝牙音箱
(6, '/uploads/products/product-6.jpg', 0, 1),
-- 商品7：智能台灯
(7, '/uploads/products/product-7.jpg', 0, 1),
-- 商品8：智能插座
(8, '/uploads/products/product-8.jpg', 0, 1),
-- 商品9：运动手环
(9, '/uploads/products/product-9.jpg', 0, 1),
-- 商品10：智能手表
(10, '/uploads/products/product-10.jpg', 0, 1),
-- 商品11：三脚架
(11, '/uploads/products/product-11.jpg', 0, 1),
-- 商品12：显示器支架
(12, '/uploads/products/product-12.jpg', 0, 1);

