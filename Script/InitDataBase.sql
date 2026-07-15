-- =============================================
-- 数据库名称：admin_db
-- 字符集：UTF-8MB4（支持emoji和特殊字符）
-- 说明：SpringBoot项目完整数据表初始化脚本
-- =============================================

-- 1. 删除旧数据库（如果存在，慎重！）
-- DROP DATABASE IF EXISTS admin_db;

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
                          avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                          email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                          phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                          status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
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

-- 3.3 菜单/权限表（树形结构，支持无限级菜单）
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                          parent_id BIGINT DEFAULT 0 COMMENT '父级ID（0表示顶级菜单）',
                          menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
                          menu_path VARCHAR(200) DEFAULT NULL COMMENT '路由路径（前端路由）',
                          menu_component VARCHAR(200) DEFAULT NULL COMMENT '组件路径（Vue组件）',
                          menu_icon VARCHAR(50) DEFAULT NULL COMMENT '菜单图标（Element Plus图标）',
                          permission_code VARCHAR(100) DEFAULT NULL COMMENT '权限编码（如：sys:user:list）',
                          menu_type TINYINT DEFAULT 1 COMMENT '类型：1目录 2菜单 3按钮',
                          sort_order INT DEFAULT 0 COMMENT '排序号（越小越靠前）',
                          status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          INDEX idx_parent_id (parent_id) COMMENT '父级索引（查询子菜单）',
                          INDEX idx_permission_code (permission_code) COMMENT '权限编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单/权限表';

-- 3.4 用户-角色关联表（多对多）
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               role_id BIGINT NOT NULL COMMENT '角色ID',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
                               PRIMARY KEY (user_id, role_id) COMMENT '联合主键',
                               INDEX idx_user_id (user_id) COMMENT '用户索引',
                               INDEX idx_role_id (role_id) COMMENT '角色索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- 3.5 角色-菜单关联表（多对多）
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
                               role_id BIGINT NOT NULL COMMENT '角色ID',
                               menu_id BIGINT NOT NULL COMMENT '菜单ID',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
                               PRIMARY KEY (role_id, menu_id) COMMENT '联合主键',
                               INDEX idx_role_id (role_id) COMMENT '角色索引',
                               INDEX idx_menu_id (menu_id) COMMENT '菜单索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

-- =============================================
-- 第二部分：业务扩展表（为商城/搜索功能预留）
-- =============================================

-- 4.1 商品分类表（无限级分类）
DROP TABLE IF EXISTS biz_category;
CREATE TABLE biz_category (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                              parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
                              category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
                              category_icon VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
                              sort_order INT DEFAULT 0 COMMENT '排序号',
                              status TINYINT DEFAULT 1 COMMENT '状态：0隐藏 1显示',
                              create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              INDEX idx_parent_id (parent_id) COMMENT '父级索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 4.2 商品SPU表（标准化产品单元）
DROP TABLE IF EXISTS biz_spu;
CREATE TABLE biz_spu (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                         spu_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'SPU编码（唯一）',
                         spu_name VARCHAR(200) NOT NULL COMMENT 'SPU名称（商品标题）',
                         category_id BIGINT NOT NULL COMMENT '分类ID',
                         brand VARCHAR(50) DEFAULT NULL COMMENT '品牌',
                         description TEXT COMMENT '商品描述',
                         main_image VARCHAR(255) DEFAULT NULL COMMENT '主图URL',
                         status TINYINT DEFAULT 1 COMMENT '状态：0下架 1上架',
                         sort_order INT DEFAULT 0 COMMENT '排序号',
                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         INDEX idx_category_id (category_id) COMMENT '分类索引',
                         INDEX idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- 4.3 商品SKU表（库存量单位，具体规格）
DROP TABLE IF EXISTS biz_sku;
CREATE TABLE biz_sku (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                         spu_id BIGINT NOT NULL COMMENT '所属SPU ID',
                         sku_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'SKU编码（唯一）',
                         sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称（如：iPhone 15 Pro 黑色 256GB）',
                         price DECIMAL(10,2) NOT NULL COMMENT '价格（分位，实际精确到分）',
                         stock INT DEFAULT 0 COMMENT '库存数量',
                         sold_count INT DEFAULT 0 COMMENT '已售数量',
                         image VARCHAR(255) DEFAULT NULL COMMENT 'SKU图片',
                         specs JSON COMMENT '规格参数（JSON格式，如{"颜色":"黑色","内存":"256GB"}）',
                         status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         INDEX idx_spu_id (spu_id) COMMENT 'SPU索引',
                         INDEX idx_price (price) COMMENT '价格索引（用于排序）',
                         INDEX idx_stock (stock) COMMENT '库存索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- =============================================
-- 第三部分：初始化数据（必需的基础数据）
-- =============================================

-- 5.1 插入管理员角色
INSERT INTO sys_role (role_code, role_name, description, status) VALUES
                                                                     ('ROLE_ADMIN', '超级管理员', '拥有所有权限', 1),
                                                                     ('ROLE_USER', '普通用户', '基础权限', 1);

-- 5.2 插入顶级菜单（一级菜单）
INSERT INTO sys_menu (parent_id, menu_name, menu_path, menu_icon, menu_type, sort_order, status) VALUES
                                                                                                     (0, '系统管理', '/system', 'Setting', 1, 1, 1),
                                                                                                     (0, '商品管理', '/product', 'Goods', 1, 2, 1),
                                                                                                     (0, '订单管理', '/order', 'Document', 1, 3, 1),
                                                                                                     (0, '数据分析', '/analysis', 'DataAnalysis', 1, 4, 1);

-- 5.3 插入系统管理子菜单（二级菜单）
INSERT INTO sys_menu (parent_id, menu_name, menu_path, menu_component, permission_code, menu_type, sort_order, status) VALUES
                                                                                                                           (1, '用户管理', '/system/user', 'system/user/index', 'sys:user:list', 2, 1, 1),
                                                                                                                           (1, '角色管理', '/system/role', 'system/role/index', 'sys:role:list', 2, 2, 1),
                                                                                                                           (1, '菜单管理', '/system/menu', 'system/menu/index', 'sys:menu:list', 2, 3, 1);

-- 5.4 插入用户管理页面的按钮权限（三级）
INSERT INTO sys_menu (parent_id, menu_name, permission_code, menu_type, sort_order, status) VALUES
                                                                                                (5, '新增用户', 'sys:user:add', 3, 1, 1),
                                                                                                (5, '编辑用户', 'sys:user:edit', 3, 2, 1),
                                                                                                (5, '删除用户', 'sys:user:delete', 3, 3, 1),
                                                                                                (5, '重置密码', 'sys:user:reset', 3, 4, 1);

-- 5.5 给管理员角色分配所有菜单权限（假设菜单ID从1到8）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
                                                 (1, 1), (1, 2), (1, 3), (1, 4),  -- 一级菜单
                                                 (1, 5), (1, 6), (1, 7),           -- 系统管理子菜单
                                                 (1, 8), (1, 9), (1, 10), (1, 11); -- 用户管理按钮

-- 5.6 给普通用户分配基础菜单（只有商品管理和部分系统管理）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
                                                 (2, 2), (2, 3);  -- 普通用户只能看到商品管理和订单管理（权限后续可细化）

-- =============================================
-- 第四部分：视图（方便日常查询，非必须）
-- =============================================

-- 6.1 用户权限视图（快速查询用户拥有的所有权限）
DROP VIEW IF EXISTS v_user_permissions;
CREATE VIEW v_user_permissions AS
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    r.role_code,
    r.role_name,
    m.permission_code,
    m.menu_name,
    m.menu_path
FROM sys_user u
         LEFT JOIN sys_user_role ur ON u.id = ur.user_id
         LEFT JOIN sys_role r ON ur.role_id = r.id
         LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
         LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE u.status = 1 AND r.status = 1 AND m.status = 1;

-- =============================================
-- 执行完成提示
-- =============================================
SELECT '✅ 数据库初始化完成！' AS 'Status';
SELECT '管理员账号：admin / 123456（密码需要在代码中BCrypt加密）' AS 'Tip';