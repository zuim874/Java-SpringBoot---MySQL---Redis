-- =============================================
-- 优惠券适用人群区分（普通用户 / VIP 用户+VIP 卖家）
-- 1. sys_coupon     增加 target_type 字段（适用人群）
-- 2. sys_user_coupon 增加 target_type 快照字段（我的优惠券展示 / 结算校验）
-- 3. 存量模板按名称语义回填（会员/尊享/VIP → VIP 券，其余 → 普通券）
-- 4. 刷新优惠券模板样本数据（幂等）
--
-- 适用人群定义：
--   target_type = 1：普通券，所有注册用户可领可用
--   target_type = 2：VIP 券，仅 VIP 用户(ROLE_VIP_USER) + VIP 卖家(ROLE_VIP_SELLER) 可领可用
-- =============================================

-- 1. sys_coupon 增加适用人群字段
ALTER TABLE sys_coupon
    ADD COLUMN target_type TINYINT NOT NULL DEFAULT 1
    COMMENT '适用人群：1全部用户（普通券） 2仅VIP（VIP券，受众=VIP用户+VIP卖家）'
    AFTER status;

-- 2. sys_user_coupon 增加适用人群快照字段
ALTER TABLE sys_user_coupon
    ADD COLUMN target_type TINYINT NOT NULL DEFAULT 1
    COMMENT '适用人群快照：1普通 2VIP专属'
    AFTER min_amount;

-- 3. 存量模板回填（会员/尊享/VIP → VIP 券，其余 → 普通券）
UPDATE sys_coupon SET target_type = 2 WHERE name LIKE '%会员%' OR name LIKE '%尊享%' OR name LIKE '%VIP%';
UPDATE sys_coupon SET target_type = 1 WHERE target_type IS NULL OR target_type = 0;

-- 4. 刷新优惠券模板样本数据（幂等，IGNORE 避免与已有主键冲突）
INSERT IGNORE INTO sys_coupon (id, name, type, discount_value, min_amount, total_count, remain_count, status, target_type) VALUES
(1, '新客满减券',            1, 5.00,  50,   10000, 10000, 1, 1),
(2, '普通用户折扣券',        2, 9.50,  100,  10000, 10000, 1, 1),
(3, '会员尊享满减券',        1, 30.00, 200,  5000,  5000,  1, 2),
(4, '会员尊享折扣券',        2, 8.50,  300,  5000,  5000,  1, 2);
