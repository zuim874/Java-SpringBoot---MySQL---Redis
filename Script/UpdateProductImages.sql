-- =============================================
-- 商品图片本地化更新脚本
-- 说明：将商品主图与轮播图从外网（images.unsplash.com）改为本地 /uploads/products/ 静态资源
-- 适用：已执行过 InitDataBase.sql、商品数据已存在的环境（无需重建表）
-- 用法：在 MySQL 中执行本脚本即可（可重复执行，结果幂等）
-- =============================================

-- ========== 1. 更新商品主图（sys_product.main_image_url） ==========
UPDATE sys_product SET main_image_url = '/uploads/products/product-1.jpg'  WHERE id = 1;
UPDATE sys_product SET main_image_url = '/uploads/products/product-2.jpg'  WHERE id = 2;
UPDATE sys_product SET main_image_url = '/uploads/products/product-3.jpg'  WHERE id = 3;
UPDATE sys_product SET main_image_url = '/uploads/products/product-4.jpg'  WHERE id = 4;
UPDATE sys_product SET main_image_url = '/uploads/products/product-5.jpg'  WHERE id = 5;
UPDATE sys_product SET main_image_url = '/uploads/products/product-6.jpg'  WHERE id = 6;
UPDATE sys_product SET main_image_url = '/uploads/products/product-7.jpg'  WHERE id = 7;
UPDATE sys_product SET main_image_url = '/uploads/products/product-8.jpg'  WHERE id = 8;
UPDATE sys_product SET main_image_url = '/uploads/products/product-9.jpg'  WHERE id = 9;
UPDATE sys_product SET main_image_url = '/uploads/products/product-10.jpg' WHERE id = 10;
UPDATE sys_product SET main_image_url = '/uploads/products/product-11.jpg' WHERE id = 11;
UPDATE sys_product SET main_image_url = '/uploads/products/product-12.jpg' WHERE id = 12;

-- ========== 2. 更新商品轮播图（product_image.image_url，按 product_id + sort 匹配） ==========
UPDATE product_image SET image_url = '/uploads/products/product-1.jpg'    WHERE product_id = 1 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-1-2.jpg'  WHERE product_id = 1 AND sort = 1;
UPDATE product_image SET image_url = '/uploads/products/product-1.jpg'    WHERE product_id = 1 AND sort = 2;
UPDATE product_image SET image_url = '/uploads/products/product-2.jpg'    WHERE product_id = 2 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-2.jpg'    WHERE product_id = 2 AND sort = 1;
UPDATE product_image SET image_url = '/uploads/products/product-3.jpg'    WHERE product_id = 3 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-3.jpg'    WHERE product_id = 3 AND sort = 1;
UPDATE product_image SET image_url = '/uploads/products/product-4.jpg'    WHERE product_id = 4 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-5.jpg'    WHERE product_id = 5 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-5.jpg'    WHERE product_id = 5 AND sort = 1;
UPDATE product_image SET image_url = '/uploads/products/product-6.jpg'    WHERE product_id = 6 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-7.jpg'    WHERE product_id = 7 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-8.jpg'    WHERE product_id = 8 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-9.jpg'    WHERE product_id = 9 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-10.jpg'   WHERE product_id = 10 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-11.jpg'   WHERE product_id = 11 AND sort = 0;
UPDATE product_image SET image_url = '/uploads/products/product-12.jpg'   WHERE product_id = 12 AND sort = 0;

-- ========== 3. 校验（应全部为 0 行残留） ==========
SELECT COUNT(*) AS '残留外网图片数' FROM sys_product WHERE main_image_url LIKE '%unsplash%';
SELECT COUNT(*) AS '残留外网轮播图数' FROM product_image WHERE image_url LIKE '%unsplash%';
