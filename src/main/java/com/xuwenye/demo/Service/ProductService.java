package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.ProductImage;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.ProductImageMapper;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品业务层
 * 1.查询：商品列表/详情/分类，均带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/更新/删除/上架/下架，写库后维护缓存
 * 3.库存操作：使用分布式锁保护扣减库存
 * <p>
 * @author ZuiM
 */
@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SellerMapper sellerMapper;
    private final RedisUtil redisUtil;
    private final RedisLockHelper redisLockHelper;
    private final MQProducer mqProducer;

    // Redis缓存Key前缀
    private static final String PRODUCT_CACHE_PREFIX = "demo:product:";
    private static final String PRODUCT_LIST_CACHE_KEY = "demo:product:list:all";
    private static final String PRODUCT_CATEGORY_CACHE_PREFIX = "demo:product:category:";
    private static final String PRODUCT_DETAIL_CACHE_PREFIX = "demo:product:detail:";
    private static final String ALL_CATEGORIES_CACHE_KEY = "demo:product:categories:all";
    private static final String SELLER_CACHE_PREFIX = "demo:seller:";

    public ProductService(ProductMapper productMapper,
                          ProductImageMapper productImageMapper,
                          SellerMapper sellerMapper,
                          RedisUtil redisUtil,
                          RedisLockHelper redisLockHelper,
                          MQProducer mqProducer) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.sellerMapper = sellerMapper;
        this.redisUtil = redisUtil;
        this.redisLockHelper = redisLockHelper;
        this.mqProducer = mqProducer;
    }

    /**
     * 获取所有上架商品（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List<Product> 上架商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> getAllOnShelfProducts() {
        // 第 1 步：先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(PRODUCT_LIST_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Product> products = productMapper.findAllOnShelfProducts();
        // 第 3 步：写入 Redis
        if (products != null && !products.isEmpty()) {
            redisUtil.set(PRODUCT_LIST_CACHE_KEY, products);
        }
        return products;
    }

    /**
     * 按分类查询上架商品（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param category 商品分类
     * @return List<Product> 指定分类的上架商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> getProductsByCategory(String category) {
        String cacheKey = PRODUCT_CATEGORY_CACHE_PREFIX + category;
        // 第 1 步：先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Product> products = productMapper.findProductsByCategory(category);
        // 第 3 步：写入 Redis
        if (products != null && !products.isEmpty()) {
            redisUtil.set(cacheKey, products);
        }
        return products;
    }

    /**
     * 按ID查询商品详情（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return Product 商品（可能为null）
     */
    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_DETAIL_CACHE_PREFIX + id;
        // 第 1 步：先从 Redis 查
        Product cached = (Product) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Product product = productMapper.findProductById(id);
        // 第 3 步：查到了就写入 Redis
        if (product != null) {
            redisUtil.set(cacheKey, product);
        }
        return product;
    }

    /**
     * 获取所有分类列表（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List<String> 分类列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getAllCategories() {
        // 第 1 步：先从 Redis 查
        List<String> cached = (List<String>) redisUtil.get(ALL_CATEGORIES_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<String> categories = productMapper.findAllCategories();
        // 第 3 步：写入 Redis
        if (categories != null && !categories.isEmpty()) {
            redisUtil.set(ALL_CATEGORIES_CACHE_KEY, categories);
        }
        return categories;
    }

    /**
     * 获取商品的所有图片（按排序）
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return List<ProductImage> 图片列表
     */
    public List<ProductImage> getProductImages(Long productId) {
        return productImageMapper.findImagesByProductId(productId);
    }

    /**
     * 获取商品主图
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return ProductImage 主图（可能为null）
     */
    public ProductImage getMainImage(Long productId) {
        return productImageMapper.findMainImageByProductId(productId);
    }

    /**
     * 保存商品（新增）
     * 1.插入数据库
     * 2.清除所有商品列表和分类缓存
     * <p>
     * @author ZuiM
     * @param product 商品实体
     * @return boolean true=保存成功
     */
    @Transactional
    public boolean saveProduct(Product product) {
        boolean result = productMapper.insert(product) > 0;
        if (result) {
            // 异步发送缓存刷新任务（不阻塞主流程）
            sendProductCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + product.getId());
            mqProducer.sendCacheRefreshTask("product", "delete", keys);
        }
        return result;
    }

    /**
     * 更新商品信息
     * 1.执行 updateById
     * 2.清除所有相关缓存
     * <p>
     * @author ZuiM
     * @param product 商品实体（必须包含id）
     * @return boolean true=更新成功
     */
    @Transactional
    public boolean updateProduct(Product product) {
        boolean result = productMapper.updateById(product) > 0;
        if (result) {
            // 异步发送缓存刷新任务（不阻塞主流程）
            sendProductCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + product.getId());
            mqProducer.sendCacheRefreshTask("product", "refresh", keys);
        }
        return result;
    }

    /**
     * 逻辑删除商品
     * 1.执行逻辑删除
     * 2.清除所有相关缓存
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteProduct(Long id) {
        boolean result = productMapper.deleteById(id) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendProductCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + id);
            mqProducer.sendCacheRefreshTask("product", "delete", keys);
        }
        return result;
    }

    /**
     * 上架商品（设置status=1）
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return boolean true=上架成功
     */
    @Transactional
    public boolean onShelfProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(1);
        boolean result = productMapper.updateById(product) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendProductCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + id);
            mqProducer.sendCacheRefreshTask("product", "refresh", keys);
        }
        return result;
    }

    /**
     * 下架商品（设置status=0）
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return boolean true=下架成功
     */
    @Transactional
    public boolean offShelfProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(0);
        boolean result = productMapper.updateById(product) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendProductCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + id);
            mqProducer.sendCacheRefreshTask("product", "refresh", keys);
        }
        return result;
    }

    /**
     * 扣减库存（使用分布式锁保护）
     * 1.获取分布式锁
     * 2.检查库存
     * 3.扣减库存
     * 4.释放锁
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return boolean true=扣减成功
     */
    @Transactional
    public boolean deductStock(Long id, int quantity) {
        String lockKey = "product:stock:" + id;
        try {
            // 获取分布式锁（看门狗模式，业务执行多久锁就持有多久）
            boolean locked = redisLockHelper.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (!locked) {
                return false; // 获取锁失败
            }

            // 查询商品
            Product product = productMapper.findProductById(id);
            if (product == null || product.getStock() < quantity) {
                return false; // 商品不存在或库存不足
            }

            // 扣减库存，增加销量
            Product update = new Product();
            update.setId(id);
            update.setStock(product.getStock() - quantity);
            update.setSold(product.getSold() + quantity);
            boolean result = productMapper.updateById(update) > 0;

            if (result) {
                // 更新成功，异步发送缓存刷新任务
                sendProductCacheRefreshTask();
                List<String> keys = new ArrayList<>();
                keys.add(PRODUCT_DETAIL_CACHE_PREFIX + id);
                mqProducer.sendCacheRefreshTask("product", "refresh", keys);
            }

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 释放锁
            redisLockHelper.unlock(lockKey);
        }
    }

    /**
     * 添加商品图片
     * <p>
     * @author ZuiM
     * @param productImage 图片实体
     * @return boolean true=添加成功
     */
    @Transactional
    public boolean addProductImage(ProductImage productImage) {
        return productImageMapper.insert(productImage) > 0;
    }

    /**
     * 删除商品图片
     * <p>
     * @author ZuiM
     * @param imageId 图片ID
     * @param productId 商品ID（用于清除缓存）
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteProductImage(Long imageId, Long productId) {
        boolean result = productImageMapper.deleteById(imageId) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            List<String> keys = new ArrayList<>();
            keys.add(PRODUCT_DETAIL_CACHE_PREFIX + productId);
            mqProducer.sendCacheRefreshTask("product", "delete", keys);
        }
        return result;
    }

    /**
     * 发送商品缓存刷新任务（异步，通过 MQ）
     * 1.清除所有商品列表缓存
     * 2.清除所有分类缓存
     * 3.清除分页缓存
     * <p>
     * @author ZuiM
     */
    private void sendProductCacheRefreshTask() {
        List<String> keys = new ArrayList<>();
        keys.add(PRODUCT_LIST_CACHE_KEY);
        keys.add(ALL_CATEGORIES_CACHE_KEY);
        // 清除所有分类缓存
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "手机配件");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "电脑外设");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "音频设备");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "智能家居");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "穿戴设备");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "摄影器材");
        keys.add(PRODUCT_CATEGORY_CACHE_PREFIX + "其他");
        // 清除分页缓存（模糊匹配）
        keys.add("demo:product:page:*");
        keys.add("demo:product:admin:page:*");
        keys.add("demo:product:seller:*");
        mqProducer.sendCacheRefreshTask("product", "clear", keys);
    }

    /**
     * 根据ID查询卖家（含 Redis 缓存）
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return Seller 卖家（可能为null）
     */
    public Seller getSellerById(Long sellerId) {
        String cacheKey = SELLER_CACHE_PREFIX + sellerId;
        // 第 1 步：先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Seller seller = sellerMapper.findSellerById(sellerId);
        // 第 3 步：写入 Redis
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }

    // ======================== 分页查询 ========================

    /**
     * 分页查询上架商品（支持按分类筛选，Cache-Aside 缓存模式）
     * 1.先从 Redis 查分页数据
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @param category 商品分类（为null则查询全部）
     * @return Page<Product> 分页商品列表
     */
    @SuppressWarnings("unchecked")
    public Page<Product> getOnShelfProductsPage(int page, int size, String category, String keyword) {
        // 缓存 key 区分按分类/关键词查询
        String cacheKey = "demo:product:page:" + page + ":" + size + ":" +
                (category != null ? category : "") + ":" +
                (keyword != null ? keyword : "");

        // 第 1 步：先从 Redis 查
        Page<Product> cached = (Page<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Page<Product> pageObj = new Page<>(page, size);
        Page<Product> result = productMapper.selectOnShelfProductsPage(pageObj, category, keyword);

        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 分页查询所有商品（包含已下架，管理员用）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<Product> 分页商品列表
     */
    @SuppressWarnings("unchecked")
    public Page<Product> getAllProductsPage(int page, int size) {
        String cacheKey = "demo:product:admin:page:" + page + ":" + size;

        // 第 1 步：先从 Redis 查
        Page<Product> cached = (Page<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Page<Product> pageObj = new Page<>(page, size);
        Page<Product> result = productMapper.selectAllProductsPage(pageObj);

        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 分页查询卖家自己的商品（包含已下架）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @param sellerId 卖家ID
     * @return Page<Product> 分页商品列表
     */
    @SuppressWarnings("unchecked")
    public Page<Product> getSellerProductsPage(int page, int size, Long sellerId) {
        String cacheKey = "demo:product:seller:" + sellerId + ":page:" + page + ":" + size;

        // 第 1 步：先从 Redis 查
        Page<Product> cached = (Page<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Page<Product> pageObj = new Page<>(page, size);
        Page<Product> result = productMapper.selectProductsBySellerIdPage(pageObj, sellerId);

        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 恢复库存（取消订单／退款时使用，分布式锁保护）
     * 1.获取分布式锁
     * 2.查询商品当前库存
     * 3.恢复库存并扣减销量
     * 4.释放锁
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @param quantity 恢复数量
     * @return boolean true=恢复成功
     */
    @Transactional
    public boolean restoreStock(Long id, int quantity) {
        String lockKey = "product:stock:" + id;
        try {
            // 获取分布式锁（看门狗模式）
            boolean locked = redisLockHelper.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }

            // 查询商品
            Product product = productMapper.findProductById(id);
            if (product == null) {
                return false;
            }

            // 恢复库存，扣减销量
            Product update = new Product();
            update.setId(id);
            update.setStock(product.getStock() + quantity);
            update.setSold(Math.max(0, product.getSold() - quantity));
            boolean result = productMapper.updateById(update) > 0;

            if (result) {
                // 更新成功，异步发送缓存刷新任务
                sendProductCacheRefreshTask();
                List<String> keys = new ArrayList<>();
                keys.add(PRODUCT_DETAIL_CACHE_PREFIX + id);
                mqProducer.sendCacheRefreshTask("product", "refresh", keys);
            }

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 释放锁
            redisLockHelper.unlock(lockKey);
        }
    }

    /**
     * 根据卖家ID查询卖家商品列表（非分页，为兼容旧接口保留）
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List<Product> 商品列表
     */
    public List<Product> getProductsBySellerId(Long sellerId) {
        return productMapper.findProductsBySellerId(sellerId);
    }
}
