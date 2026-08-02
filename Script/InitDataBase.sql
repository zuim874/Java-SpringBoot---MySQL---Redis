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

-- 3.3 商品表
DROP TABLE IF EXISTS sys_product;
CREATE TABLE sys_product (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                             seller_id BIGINT NOT NULL COMMENT '卖家ID，关联sys_seller.id',
                             product_name VARCHAR(50) NOT NULL COMMENT '商品名称',
                             price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品价格(元)',
                             stock INT NOT NULL DEFAULT 0 COMMENT '商品库存数量',
                             sold INT NOT NULL DEFAULT 0 COMMENT '已售出数量',
                             status TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态：0下架 1上架',
                             description VARCHAR(255) NOT NULL COMMENT '商品描述',
                             main_image_url VARCHAR(255) NULL COMMENT '冗余：商品主图URL，列表页查询优化',
                             is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                             create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                             update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP,
                             INDEX idx_seller_id (seller_id),
                             INDEX idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 3.4 卖家表
DROP TABLE IF EXISTS sys_seller;
CREATE TABLE sys_seller (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                            seller_name VARCHAR(50) NOT NULL COMMENT '卖家名称',
                            address VARCHAR(255) NOT NULL COMMENT '卖家地址',
                            is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
                            create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                            update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP,
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

