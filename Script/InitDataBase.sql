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
                          is_deleted TINYINT DEFAULT 1 COMMENT '状态：0删除 1未删除',
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


-- 5.1 插入管理员角色
INSERT INTO sys_role (role_code, role_name, description, status) VALUES
                                                                     ('ROLE_ADMIN', '超级管理员', '拥有所有权限', 1),
                                                                     ('ROLE_USER', '普通用户', '基础权限', 1);

