-- =============================================
-- 数据库名称：springbootdb
-- 字符集：UTF-8MB4（支持emoji和特殊字符）
-- 说明：SpringBoot项目【初始化数据】脚本（仅 DML，不含任何建表语句）
-- 前置：需先执行同目录下的 InitDataBase.sql（完成建库建表）
-- 已合并：角色（5条）/ 卖家（22条）/ 商品分类（15条）
--         商品（65条）/ 商品图片（全部）/ 优惠券模板（4条）
-- 注意：
--   1. 本脚本不包含任何用户账号；管理员与卖家登录账号由
--      项目启动时的 InitAdminRunner / InitSellerAccountsRunner 自动创建
--      （管理员 admin/123456；卖家账号=店铺名称/默认密码 Seller@123）
--   2. 商品分类以 sys_category.id 存储（英文逗号分隔，支持多分类）
--      分类ID：1手机配件 2电脑外设 3音频设备 4智能家居 5穿戴设备
--              6摄影器材 7食品饮料 8服装鞋帽 9图书文具 10家居生活
--              11运动户外 12母婴用品 13美妆护肤 14宠物用品 15其他
-- 用法：新环境依次执行 InitDataBase.sql → 本文件
-- =============================================

USE springbootdb;

-- =============================================
-- 第一部分：角色数据
-- =============================================
INSERT IGNORE INTO sys_role (id, role_code, role_name, description, status) VALUES
                                                                     (1, 'ROLE_ADMIN', '超级管理员', '拥有所有权限', 1),
                                                                     (2, 'ROLE_USER', '普通用户', '基础权限', 1),
                                                                     (3, 'ROLE_VIP_USER', '会员用户', '会员用户权限', 1),
                                                                     (4, 'ROLE_SELLER', '普通卖家', '普通卖家权限', 1),
                                                                     (5, 'ROLE_VIP_SELLER', '会员卖家', '会员卖家权限', 1);

-- =============================================
-- 第二部分：卖家数据（共22个卖家，覆盖全部品类）
-- =============================================
INSERT IGNORE INTO sys_seller (id, seller_name, seller_avatar, seller_contact, address) VALUES
-- 电子产品类卖家（ID 1-5）
(1,  'ZuiM官方旗舰店',         '/uploads/sellers/default.png',   '400-888-0001', '广东省深圳市南山区科技园'),
(2,  '数码优选专营店',         '/uploads/sellers/default.png',   '400-888-0002', '北京市海淀区中关村大街'),
(3,  '潮玩科技馆',             '/uploads/sellers/default.png',   '400-888-0003', '上海市浦东新区张江高科技园区'),
(4,  '品质生活家居馆',         '/uploads/sellers/default.png',   '400-888-0004', '浙江省杭州市余杭区未来科技城'),
(5,  '摄影器材总汇',           '/uploads/sellers/default.png',   '400-888-0005', '广东省广州市天河区珠江新城'),
-- 食品饮料类卖家
(6,  '鲜味坊食品专营店',       '/uploads/sellers/seller-06.png', '400-888-0006', '四川省成都市武侯区人民南路四段'),
(14, '有机生活坊',             '/uploads/sellers/seller-14.png', '400-888-0014', '云南省昆明市盘龙区北京路'),
(22, '品味食尚食品店',         '/uploads/sellers/seller-22.png', '400-888-0022', '福建省厦门市思明区中山路'),
-- 服装鞋帽类卖家
(7,  '风尚服饰旗舰店',         '/uploads/sellers/seller-07.png', '400-888-0007', '浙江省杭州市江干区四季青服装城'),
(15, '潮流前线服饰店',         '/uploads/sellers/seller-15.png', '400-888-0015', '广东省广州市白云区三元里大道'),
-- 图书文具类卖家
(8,  '墨香书屋图书专营店',     '/uploads/sellers/seller-08.png', '400-888-0008', '北京市朝阳区建国路88号'),
(16, '智慧书房文具店',         '/uploads/sellers/seller-16.png', '400-888-0016', '江苏省南京市鼓楼区湖南路'),
-- 家居生活类卖家
(9,  '温馨家居生活馆',         '/uploads/sellers/seller-09.png', '400-888-0009', '浙江省杭州市余杭区良渚街道'),
(17, '雅致生活家居馆',         '/uploads/sellers/seller-17.png', '400-888-0017', '广东省佛山市顺德区乐从镇'),
-- 运动户外类卖家
(10, '动感户外旗舰店',         '/uploads/sellers/seller-10.png', '400-888-0010', '北京市东城区王府井大街'),
(18, '极限运动户外店',         '/uploads/sellers/seller-18.png', '400-888-0018', '广东省深圳市龙岗区坂田街道'),
-- 母婴用品类卖家
(11, '亲亲宝贝母婴店',         '/uploads/sellers/seller-11.png', '400-888-0011', '上海市徐汇区漕溪北路'),
(20, '阳光宝贝母婴店',         '/uploads/sellers/seller-20.png', '400-888-0020', '湖南省长沙市岳麓区梅溪湖路'),
-- 美妆护肤类卖家
(12, '美丽日记美妆护肤店',     '/uploads/sellers/seller-12.png', '400-888-0012', '上海市静安区南京西路'),
(21, '自然之美护肤馆',         '/uploads/sellers/seller-21.png', '400-888-0021', '山东省青岛市市南区香港中路'),
-- 宠物用品类卖家
(13, '宠爱有家宠物用品店',     '/uploads/sellers/seller-13.png', '400-888-0013', '湖北省武汉市洪山区珞喻路'),
(19, '萌宠乐园宠物店',         '/uploads/sellers/seller-19.png', '400-888-0019', '重庆市渝北区龙溪街道');

-- =============================================
-- 第三部分：商品分类数据（管理员维护的预设分类）
-- =============================================
INSERT IGNORE INTO sys_category (id, name, sort, status) VALUES
(1,  '手机配件', 10, 1),
(2,  '电脑外设', 20, 1),
(3,  '音频设备', 30, 1),
(4,  '智能家居', 40, 1),
(5,  '穿戴设备', 50, 1),
(6,  '摄影器材', 60, 1),
(7,  '食品饮料', 70, 1),
(8,  '服装鞋帽', 80, 1),
(9,  '图书文具', 90, 1),
(10, '家居生活', 100, 1),
(11, '运动户外', 110, 1),
(12, '母婴用品', 120, 1),
(13, '美妆护肤', 130, 1),
(14, '宠物用品', 140, 1),
(15, '其他',     150, 1);

-- =============================================
-- 第四部分：商品数据（共65个商品，覆盖15个分类）
-- 说明：category 列存储分类ID（英文逗号分隔，支持多分类）
-- =============================================
INSERT IGNORE INTO sys_product (id, seller_id, product_name, price, stock, sold, status, description, category, main_image_url) VALUES
-- ============ 手机配件（seller_id=1） ============
(1, 1,  '极速无线充电器',                  129.00, 200, 156, 1, '15W 快充 · 兼容 iPhone/Android · 轻薄便携', '1', '/uploads/products/product-1.jpg'),
(2, 1,  '防摔手机壳 磁吸系列',              49.00,  500, 380, 1, '军工级防摔 · 磁吸兼容 · 多色可选 · 手感细腻', '1', '/uploads/products/product-2.jpg'),
-- ============ 电脑外设（seller_id=2） ============
(3, 2,  '机械键盘 K8 Pro',                  399.00, 100, 78, 1, '87键 · 青轴 · RGB背光 · 铝合金机身', '2', '/uploads/products/product-3.jpg'),
(4, 2,  'USB-C 扩展坞 HubMax',              259.00, 150, 112, 1, '12合1 · 4K60Hz · 100W PD · 千兆网口', '2', '/uploads/products/product-4.jpg'),
(61, 2, '无线静音鼠标',                     99.00,  350, 567, 1, '2.4G+蓝牙双模 · 静音按键 · 人体工学设计 · 续航12个月 · 便携收纳', '2', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&h=800&fit=crop'),
-- ============ 音频设备（seller_id=3） ============
(5, 3,  '降噪耳机 AirSound',                599.00, 80, 65, 1, '主动降噪 · 40h续航 · 蓝牙5.3 · Hi-Res认证', '3', '/uploads/products/product-5.jpg'),
(6, 3,  '蓝牙音箱 MiniBeat',                179.00, 120, 89, 1, '360°环绕声 · 12h续航 · IPX5防水 · 小巧便携', '3', '/uploads/products/product-6.jpg'),
(60, 3, '主动降噪蓝牙耳机',                 449.00, 120, 234, 1, 'ANC主动降噪 · 蓝牙5.3 · 30h续航 · 入耳式 · 支持无线充电', '3', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop'),
(63, 1, '便携式蓝牙音箱',                   229.00, 150, 198, 1, 'IPX7级防水 · 20h续航 · 蓝牙5.0 · TWS串联 · 户外露营派对必备', '3', 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&h=800&fit=crop'),
-- ============ 智能家居（seller_id=4） ============
(7, 4,  '智能台灯 Lumina',                  249.00, 90, 67, 1, '无级调光 · 色温调节 · 护眼模式 · 智能联动', '4', '/uploads/products/product-7.jpg'),
(8, 4,  '智能插座 SmartPlug',               89.00,  300, 220, 1, '远程控制 · 电量统计 · 语音控制 · 定时开关', '4', '/uploads/products/product-8.jpg'),
(62, 4, '智能声波电动牙刷',                 199.00, 180, 312, 1, '31000次/分钟声波振动 · 5种清洁模式 · 无线快充 · IPX7防水 · 送刷头*4', '4', 'https://images.unsplash.com/photo-1559467273-0e8c4e4b6b0e?w=800&h=800&fit=crop'),
-- ============ 穿戴设备（seller_id=5） ============
(9, 5,  '运动手环 FitBand',                 199.00, 180, 145, 1, '心率监测 · 睡眠分析 · IP68防水 · 14天续航', '5', '/uploads/products/product-9.jpg'),
(10, 5, '智能手表 WatchX',                  899.00, 60, 42, 1, 'AMOLED屏 · eSIM · 7天续航 · 血氧监测', '5', '/uploads/products/product-10.jpg'),
(64, 5, '智能体脂秤',                       79.00,  400, 623, 1, 'BIA生物电阻抗 · 15项身体数据 · 手机APP同步 · 钢化玻璃面板 · 超薄设计', '5', 'https://images.unsplash.com/photo-1559268950-2d4e5c1f1b0e?w=800&h=800&fit=crop'),
-- ============ 摄影器材（seller_id=1） ============
(11, 1, '便携三脚架 ProPod',                159.00, 75, 58, 1, '碳纤维 · 1.5kg承重 · 折叠便携 · 快装板', '6', '/uploads/products/product-11.jpg'),
(65, 5, '手持云台稳定器',                   599.00, 60, 89, 1, '三轴防抖 · 手机/运动相机通用 · 智能跟随 · 蓝牙遥控 · Vlog拍摄神器', '6', 'https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=800&h=800&fit=crop'),
-- ============ 食品饮料（6个商品，卖家：6/14/22） ============
(13, 6,  '有机坚果礼盒 500g',               128.00, 300, 425, 1, '精选6种有机坚果 · 每日坚果标准 · 独立小包 · 年货送礼首选', '7', 'https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=800&h=800&fit=crop'),
(14, 6,  '进口阿拉比卡咖啡豆 250g',         89.00,  200, 312, 1, '哥伦比亚慧兰产区 · 中度烘焙 · 单品手冲 · 风味层次丰富', '7', 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=800&h=800&fit=crop'),
(15, 14, '明前龙井茶叶 100g 礼盒装',         268.00, 100, 186, 1, '西湖核心产区 · 明前采摘 · 一芽一叶 · 豆香馥郁 · 精美礼盒包装', '7', 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=800&h=800&fit=crop'),
(16, 22, '手工松露巧克力礼盒 16枚装',       168.00, 150, 238, 1, '比利时进口可可脂 · 法式手工制作 · 四种口味 · 丝滑入口即化', '7', 'https://images.unsplash.com/photo-1606312619070-d48b4c652a52?w=800&h=800&fit=crop'),
(17, 14, '云南古树普洱茶饼 357g',           358.00, 80,  156, 1, '勐海布朗山古树原料 · 2023年春茶 · 生普 · 回甘生津 · 越陈越香', '7', 'https://images.unsplash.com/photo-1563822249366-3efb23b8e0c9?w=800&h=800&fit=crop'),
(18, 22, '比利时黑巧克力礼盒 12枚装',       138.00, 200, 189, 1, '72%可可含量 · 低糖健康 · 精美铁盒包装 · 情人节送礼佳品', '7', 'https://images.unsplash.com/photo-1587139223877-04cb899fa3e8?w=800&h=800&fit=crop'),
-- ============ 服装鞋帽（6个商品，卖家：7/15） ============
(19, 7,  '轻便休闲运动鞋',                  259.00, 250, 458, 1, '飞织网面 · 透气轻便 · 记忆棉鞋垫 · 防滑橡胶底 · 男女同款', '8', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop'),
(20, 7,  '商务修身免烫衬衫',                199.00, 180, 325, 1, '长绒棉面料 · 免烫抗皱 · 修身剪裁 · 舒适透气 · 适合商务办公', '8', 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800&h=800&fit=crop'),
(21, 15, '时尚防水石英手表',                329.00, 120, 198, 1, '30米防水 · 矿物强化玻璃镜面 · 不锈钢表带 · 日历显示 · 经典百搭', '8', 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop'),
(22, 15, '偏光防紫外线太阳镜',              159.00, 300, 267, 1, 'TAC偏光镜片 · UV400防紫外线 · TR90超轻镜框 · 附赠眼镜盒', '8', 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop'),
(23, 7,  '纯棉圆领短袖T恤',                 79.00,  500, 623, 1, '100%新疆长绒棉 · 260g重磅面料 · 不变形不缩水 · 多色可选', '8', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=800&fit=crop'),
(24, 15, '真丝家居睡衣套装',                399.00, 80,  112, 1, '19姆米桑蚕丝 · 透气亲肤 · 简约剪裁 · 礼盒包装 · 居家必备', '8', 'https://images.unsplash.com/photo-1614192379094-7e2b02e75c4b?w=800&h=800&fit=crop'),
-- ============ 图书文具（6个商品，卖家：8/16） ============
(25, 8,  '《百年孤独》精装纪念版',           55.00,  400, 589, 1, '加西亚·马尔克斯代表作 · 精装硬壳 · 珍藏插图版 · 诺贝尔文学奖经典', '9', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&h=800&fit=crop'),
(26, 8,  '《Python编程从入门到实践》第3版',  89.00,  350, 478, 1, '零基础学编程首选 · 涵盖Python基础与项目实战 · 附赠源码与视频教程', '9', 'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=800&h=800&fit=crop'),
(27, 8,  '《三体》全集典藏版',              128.00, 280, 432, 1, '刘慈欣科幻巨著 · 三体全三册 · 精装典藏 · 雨果奖获奖作品', '9', 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&h=800&fit=crop'),
(28, 16, '凌美 Safari 狩猎者钢笔套装',      198.00, 150, 213, 1, '德国品牌 · 笔握人体工学设计 · 不锈钢笔尖 · 附赠墨胆5支 · 送礼佳品', '9', 'https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?w=800&h=800&fit=crop'),
(29, 16, '复古牛皮手账本礼盒套装',           68.00,  200, 345, 1, '真牛皮封面 · 180°平摊 · 方格内页 · 含钢笔+贴纸+书签 · 文艺范十足', '9', 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=800&h=800&fit=crop'),
(30, 16, '儿童水彩笔 48色套装',              59.00,  300, 412, 1, '可水洗配方 · 安全无毒 · 三角笔杆 · 色彩鲜艳 · 美术启蒙必备', '9', 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800&h=800&fit=crop'),
-- ============ 家居生活（6个商品，卖家：9/17） ============
(31, 9,  '法国薰衣草香薰蜡烛礼盒',           69.00,  250, 387, 1, '天然大豆蜡 · 普罗旺斯薰衣草精油 · 燃烧时长40h+ · 精美礼盒包装', '10', 'https://images.unsplash.com/photo-1602523961358-f9f03b2d0e9e?w=800&h=800&fit=crop'),
(32, 9,  '纯棉印花床上四件套',              299.00, 150, 278, 1, '60支长绒棉 · 亲肤透气 · 简约印花 · 包含被套床单枕套 · 1.8m床适用', '10', 'https://images.unsplash.com/photo-1616486029423-aaa4789e8c9a?w=800&h=800&fit=crop'),
(33, 17, '不锈钢厨房刀具7件套',              159.00, 120, 198, 1, '德国不锈钢 · 锋利耐用 · 防滑手柄 · 含斩骨刀/切片刀/水果刀/剪刀/磨刀棒/刀座', '10', 'https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?w=800&h=800&fit=crop'),
(34, 17, '日式PP收纳盒三件套',               49.00,  500, 678, 1, '加厚PP材质 · 带盖防尘 · 可叠放设计 · 换季衣物收纳 · 衣柜整理神器', '10', 'https://images.unsplash.com/photo-1621899230950-3e3c0e1e6f3e?w=800&h=800&fit=crop'),
(35, 9,  '智能感应垃圾桶 12L',               89.00,  180, 234, 1, '红外感应开盖 · 0.3秒响应 · 静音缓降 · IPX5防水 · USB充电', '10', 'https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop'),
(36, 17, '厨房电子秤 精度0.1g',              39.00,  350, 456, 1, '高精度传感器 · 最大称重5kg · 单位切换 · 去皮功能 · 烘焙必备', '10', 'https://images.unsplash.com/photo-1589903308904-1010c2294adc?w=800&h=800&fit=crop'),
-- ============ 运动户外（6个商品，卖家：10/18） ============
(37, 10, 'TPE环保瑜伽垫 6mm',                79.00,  300, 534, 1, 'TPE环保材质 · 双面防滑 · 6mm加厚 · 附带背带 · 初学者进阶均适用', '11', 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop'),
(38, 10, '户外跑步轻量双肩包 15L',           149.00, 200, 312, 1, '防泼水面料 · 蜂巢减负背板 · 多隔层收纳 · 可挂登山杖 · 适合越野跑', '11', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop'),
(39, 18, '316不锈钢真空保温杯 500ml',        99.00,  350, 567, 1, '316医用级不锈钢 · 12h保温保冷 · 真空隔热 · 食品级硅胶密封圈', '11', 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop'),
(40, 10, '速开全自动帐篷 3-4人',             399.00, 80,  145, 1, '全自动速开设计 · 防雨防晒UPF50+ · 双层加厚 · 含地钉风绳 · 野营露营必备', '11', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&h=800&fit=crop'),
(41, 18, '碳纤维超轻登山杖 一对装',          129.00, 150, 213, 1, '航空级碳纤维 · 超轻190g/根 · 可调节长度 · 避震系统 · 含泥托杖尖', '11', 'https://images.unsplash.com/photo-1633699734050-0bc1b5cac3e3?w=800&h=800&fit=crop'),
(42, 18, '运动相机 4K高清 防抖版',           1299.00, 50,  89,  1, '4K60fps · 六轴防抖 · 10米防水 · 双屏显示 · 含支架+电池*2 · 骑行vlog必备', '11', 'https://images.unsplash.com/photo-1572569362234-6b5b1e5e4d4a?w=800&h=800&fit=crop'),
-- ============ 母婴用品（5个商品，卖家：11/20） ============
(43, 11, '轻便高景观婴儿推车',              699.00, 60,  123, 1, '单手一键收车 · 可坐可躺 · 高景观远离尾气 · 四轮避震 · 大容量置物篮', '12', 'https://images.unsplash.com/photo-1593510987046-1f7f5f0b9c8a?w=800&h=800&fit=crop'),
(44, 11, '儿童早教益智积木 100粒',           89.00,  200, 312, 1, '大颗粒防吞咽 · 环保ABS材质 · 兼容乐高 · 含收纳盒 · 培养动手创造力', '12', 'https://images.unsplash.com/photo-1587654780291-39c9404d746b?w=800&h=800&fit=crop'),
(45, 20, '幼儿启蒙绘本套装 30册',            128.00, 180, 289, 1, '中英双语 · 铜版纸彩印 · 圆角不伤手 · 涵盖认知/习惯/情绪管理 · 0-3岁适用', '12', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&h=800&fit=crop'),
(46, 11, '婴儿安全防护围栏 14片+爬行垫',     358.00, 80,  156, 1, 'HDPE环保材质 · 加厚防撞 · 多角度固定 · 含游戏门 · 给宝宝安全活动空间', '12', 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1b0?w=800&h=800&fit=crop'),
(47, 20, '儿童保温吸管杯 350ml',             69.00,  300, 456, 1, '316不锈钢内胆 · 6h保温 · V型吸管防喷溅 · 卡通图案 · 食品级硅胶吸管', '12', 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop'),
-- ============ 美妆护肤（6个商品，卖家：12/21） ============
(48, 12, '玻尿酸补水保湿面膜 20片装',        89.00,  400, 678, 1, '三重玻尿酸 · 天丝膜布 · 轻薄服帖 · 深层补水 · 敏感肌适用', '13', 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop'),
(49, 12, '烟酰胺亮肤精华液 30ml',            159.00, 200, 345, 1, '5%烟酰胺 · 377美白成分 · 淡化痘印 · 提亮肤色 · 清爽不黏腻', '13', 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800&h=800&fit=crop'),
(50, 21, '清爽防晒霜 SPF50+ PA+++',          79.00,  350, 523, 1, '物化结合防晒 · 清爽不油腻 · 不假白 · 防水防汗 · 军训/户外必备', '13', 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop'),
(51, 21, '邂逅淡香水 EDT 50ml',              268.00, 120, 198, 1, '花香调 · 前调柑橘/茉莉 · 中调玫瑰/鸢尾 · 后调琥珀/麝香 · 留香持久', '13', 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=800&h=800&fit=crop'),
(52, 12, '氨基酸温和洁面乳 120g',             59.00,  400, 689, 1, '氨基酸表活 · 弱酸性配方 · 温和不紧绷 · 适合所有肤质 · 每天早晚可用', '13', 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop'),
(53, 21, '维生素C亮肤面膜 10片装',            69.00,  300, 412, 1, 'VC衍生物+玻尿酸 · 提亮熬夜肌 · 补水保湿 · 天然棉膜布 · 每周2-3次', '13', 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=800&h=800&fit=crop'),
-- ============ 宠物用品（6个商品，卖家：13/19） ============
(54, 13, '天然无谷猫粮 2kg',                 128.00, 200, 389, 1, '鸡肉三文鱼配方 · 无谷低敏 · 含Omega-3 · 美毛护肠胃 · 全年龄段适用', '14', 'https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=800&h=800&fit=crop'),
(55, 13, '舒适保暖可拆洗狗窝 L号',           89.00,  150, 234, 1, '短毛绒面料 · 加厚填充 · 可拆洗设计 · 防潮底垫 · 适合10-20kg中型犬', '14', 'https://images.unsplash.com/photo-1591946614720-90a587da4a36?w=800&h=800&fit=crop'),
(56, 19, '互动发声宠物玩具 6件套',           39.00,  500, 678, 1, '耐咬橡胶 · 内置发声器 · 锻炼咀嚼 · 增进互动 · 适合中大型犬', '14', 'https://images.unsplash.com/photo-1568572933382-74d440642117?w=800&h=800&fit=crop'),
(57, 19, '智能定时自动喂食器',               199.00, 100, 167, 1, 'WiFi远程控制 · 定时定量出粮 · 304不锈钢食盆 · 支持语音录制 · 缺粮预警', '14', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&h=800&fit=crop'),
(58, 13, '大型豪华猫爬架',                   299.00, 60,  98,  1, '剑麻柱+绒布平台 · 多层级设计 · 含吊床/猫窝 · 可承重15kg · 猫咪乐园', '14', 'https://images.unsplash.com/photo-1545249390-6bdfa286032f?w=800&h=800&fit=crop'),
(59, 19, '宠物随行水碗 500ml',               49.00,  250, 312, 1, '硅胶折叠设计 · 随身携带 · 防漏密封 · 食品级材质 · 遛狗必备', '14', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&h=800&fit=crop'),
-- ============ 补充电子产品（seller_id=1/2） ============
(12, 2,  '显示器支架 ArmOne',                299.00, 65, 48, 1, '气动悬臂 · 17-32寸 · 理线设计 · 桌面夹式', '2', '/uploads/products/product-12.jpg');

-- =============================================
-- 第五部分：商品图片数据（商品1-12：本地图片；商品13-65：外网图片）
-- =============================================
INSERT IGNORE INTO product_image (product_id, image_url, sort, is_main) VALUES
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
(12, '/uploads/products/product-12.jpg', 0, 1),
-- 商品13：有机坚果礼盒
(13, 'https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=800&h=800&fit=crop', 0, 1),
(13, 'https://images.unsplash.com/photo-1566487766-8e18f9d1e0b0?w=800&h=800&fit=crop', 1, 0),
-- 商品14：进口阿拉比卡咖啡豆
(14, 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=800&h=800&fit=crop', 0, 1),
(14, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefda?w=800&h=800&fit=crop', 1, 0),
-- 商品15：明前龙井茶叶
(15, 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=800&h=800&fit=crop', 0, 1),
-- 商品16：手工松露巧克力
(16, 'https://images.unsplash.com/photo-1606312619070-d48b4c652a52?w=800&h=800&fit=crop', 0, 1),
(16, 'https://images.unsplash.com/photo-1549007994-cb92caebd54b?w=800&h=800&fit=crop', 1, 0),
-- 商品17：云南古树普洱茶饼
(17, 'https://images.unsplash.com/photo-1563822249366-3efb23b8e0c9?w=800&h=800&fit=crop', 0, 1),
-- 商品18：比利时黑巧克力
(18, 'https://images.unsplash.com/photo-1587139223877-04cb899fa3e8?w=800&h=800&fit=crop', 0, 1),
-- 商品19：休闲运动鞋
(19, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop', 0, 1),
(19, 'https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&h=800&fit=crop', 1, 0),
-- 商品20：商务衬衫
(20, 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800&h=800&fit=crop', 0, 1),
-- 商品21：时尚手表
(21, 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop', 0, 1),
(21, 'https://images.unsplash.com/photo-1542496658-e33a6d0d50f6?w=800&h=800&fit=crop', 1, 0),
-- 商品22：太阳镜
(22, 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop', 0, 1),
-- 商品23：纯棉T恤
(23, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=800&fit=crop', 0, 1),
-- 商品24：真丝睡衣
(24, 'https://images.unsplash.com/photo-1614192379094-7e2b02e75c4b?w=800&h=800&fit=crop', 0, 1),
-- 商品25：《百年孤独》
(25, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&h=800&fit=crop', 0, 1),
-- 商品26：《Python编程》
(26, 'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=800&h=800&fit=crop', 0, 1),
-- 商品27：《三体》
(27, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&h=800&fit=crop', 0, 1),
-- 商品28：凌美钢笔套装
(28, 'https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?w=800&h=800&fit=crop', 0, 1),
(28, 'https://images.unsplash.com/photo-1585338107529-1c2b6f6b9e3e?w=800&h=800&fit=crop', 1, 0),
-- 商品29：复古手账本
(29, 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=800&h=800&fit=crop', 0, 1),
-- 商品30：儿童水彩笔
(30, 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800&h=800&fit=crop', 0, 1),
-- 商品31：香薰蜡烛
(31, 'https://images.unsplash.com/photo-1602523961358-f9f03b2d0e9e?w=800&h=800&fit=crop', 0, 1),
(31, 'https://images.unsplash.com/photo-1605617266744-1ac8b0e0a7b0?w=800&h=800&fit=crop', 1, 0),
-- 商品32：床上四件套
(32, 'https://images.unsplash.com/photo-1616486029423-aaa4789e8c9a?w=800&h=800&fit=crop', 0, 1),
-- 商品33：厨房刀具套装
(33, 'https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?w=800&h=800&fit=crop', 0, 1),
-- 商品34：收纳盒三件套
(34, 'https://images.unsplash.com/photo-1621899230950-3e3c0e1e6f3e?w=800&h=800&fit=crop', 0, 1),
-- 商品35：智能垃圾桶
(35, 'https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop', 0, 1),
-- 商品36：厨房电子秤
(36, 'https://images.unsplash.com/photo-1589903308904-1010c2294adc?w=800&h=800&fit=crop', 0, 1),
-- 商品37：瑜伽垫
(37, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop', 0, 1),
(37, 'https://images.unsplash.com/photo-1605051396429-0efc9c8c0e5b?w=800&h=800&fit=crop', 1, 0),
-- 商品38：跑步背包
(38, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop', 0, 1),
-- 商品39：保温杯
(39, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop', 0, 1),
-- 商品40：帐篷
(40, 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&h=800&fit=crop', 0, 1),
(40, 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=800&h=800&fit=crop', 1, 0),
-- 商品41：登山杖
(41, 'https://images.unsplash.com/photo-1633699734050-0bc1b5cac3e3?w=800&h=800&fit=crop', 0, 1),
-- 商品42：运动相机
(42, 'https://images.unsplash.com/photo-1572569362234-6b5b1e5e4d4a?w=800&h=800&fit=crop', 0, 1),
(42, 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop', 1, 0),
-- 商品43：婴儿推车
(43, 'https://images.unsplash.com/photo-1593510987046-1f7f5f0b9c8a?w=800&h=800&fit=crop', 0, 1),
-- 商品44：益智积木
(44, 'https://images.unsplash.com/photo-1587654780291-39c9404d746b?w=800&h=800&fit=crop', 0, 1),
-- 商品45：儿童绘本
(45, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&h=800&fit=crop', 0, 1),
-- 商品46：安全围栏
(46, 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1b0?w=800&h=800&fit=crop', 0, 1),
-- 商品47：儿童保温杯
(47, 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop', 0, 1),
-- 商品48：补水面膜
(48, 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop', 0, 1),
(48, 'https://images.unsplash.com/photo-1616394584738-fc6e612e71b9?w=800&h=800&fit=crop', 1, 0),
-- 商品49：烟酰胺精华液
(49, 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800&h=800&fit=crop', 0, 1),
-- 商品50：防晒霜
(50, 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop', 0, 1),
-- 商品51：淡香水
(51, 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=800&h=800&fit=crop', 0, 1),
-- 商品52：洁面乳
(52, 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&h=800&fit=crop', 0, 1),
-- 商品53：VC面膜
(53, 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=800&h=800&fit=crop', 0, 1),
-- 商品54：无谷猫粮
(54, 'https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=800&h=800&fit=crop', 0, 1),
-- 商品55：狗窝
(55, 'https://images.unsplash.com/photo-1591946614720-90a587da4a36?w=800&h=800&fit=crop', 0, 1),
-- 商品56：宠物玩具
(56, 'https://images.unsplash.com/photo-1568572933382-74d440642117?w=800&h=800&fit=crop', 0, 1),
-- 商品57：自动喂食器
(57, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&h=800&fit=crop', 0, 1),
-- 商品58：猫爬架
(58, 'https://images.unsplash.com/photo-1545249390-6bdfa286032f?w=800&h=800&fit=crop', 0, 1),
-- 商品59：随行水碗
(59, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&h=800&fit=crop', 0, 1),
-- 商品60：降噪蓝牙耳机
(60, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop', 0, 1),
(60, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?w=800&h=800&fit=crop', 1, 0),
-- 商品61：无线鼠标
(61, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&h=800&fit=crop', 0, 1),
-- 商品62：电动牙刷
(62, 'https://images.unsplash.com/photo-1559467273-0e8c4e4b6b0e?w=800&h=800&fit=crop', 0, 1),
-- 商品63：蓝牙音箱
(63, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&h=800&fit=crop', 0, 1),
(63, 'https://images.unsplash.com/photo-1589003077984-894e133dabab?w=800&h=800&fit=crop', 1, 0),
-- 商品64：智能体脂秤
(64, 'https://images.unsplash.com/photo-1559268950-2d4e5c1f1b0e?w=800&h=800&fit=crop', 0, 1),
-- 商品65：手持稳定器
(65, 'https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=800&h=800&fit=crop', 0, 1);

-- =============================================
-- 第六部分：优惠券模板示例数据
-- 普通买家券：力度较小；会员买家券：力度更大
-- =============================================
INSERT IGNORE INTO sys_coupon (id, name, type, discount_value, min_amount, total_count, remain_count, status, target_type) VALUES
(1, '新客满减券',            1, 5.00,  50,   10000, 10000, 1, 1),
(2, '普通用户折扣券',        2, 9.50,  100,  10000, 10000, 1, 1),
(3, '会员尊享满减券',        1, 30.00, 200,  5000,  5000,  1, 2),
(4, '会员尊享折扣券',        2, 8.50,  300,  5000,  5000,  1, 2);
