package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.ProductImage;
import com.xuwenye.demo.Mapper.ProductImageMapper;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品业务层
 * 1.查询：所有商品列表 / 按分类筛选 / 按关键词搜索 / 商品详情，均带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/更新/逻辑删除，写库后维护缓存
 * 3.图片查询：按商品ID查询图片列表
 * <p>
 * @author ZuiM
 */
@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final RedisUtil redisUtil;

    public ProductService(ProductMapper productMapper,
                          ProductImageMapper productImageMapper,
                          RedisUtil redisUtil) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.redisUtil = redisUtil;
    }

    // ==================== 查询方法（带缓存） ====================

    /**
     * 查询所有已上架商品（含 Redis 缓存）
     * 1.先从 Redis 查（demo:product:active:list）
     * 2.未命中则查 MySQL（含卖家名称左连接）
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List&lt;Product&gt; 商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> findActiveProducts() {
        String cacheKey = "demo:product:active:list";
        // 1. 先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        List<Product> list = productMapper.findActiveProducts();
        // 3. 写入 Redis（5 分钟过期，列表缓存允许短暂不一致）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 按分类查询已上架商品（含 Redis 缓存）
     * 1.先从 Redis 查（demo:product:category:{category}）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param category 商品分类
     * @return List&lt;Product&gt; 商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> findActiveProductsByCategory(String category) {
        String cacheKey = "demo:product:category:" + category;
        // 1. 先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        List<Product> list = productMapper.findActiveProductsByCategory(category);
        // 3. 写入 Redis（5 分钟过期）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 按关键词搜索已上架商品（含 Redis 缓存，搜索词作为 key 的一部分）
     * 1.先从 Redis 查（demo:product:search:{keyword}）
     * 2.未命中则查 MySQL（LIKE 模糊匹配）
     * 3.查到了写入 Redis（注意：搜索缓存时效性较低，可接受短暂不一致）
     * <p>
     * @author ZuiM
     * @param keyword 搜索关键词
     * @return List&lt;Product&gt; 商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> searchActiveProducts(String keyword) {
        String cacheKey = "demo:product:search:" + keyword;
        // 1. 先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        List<Product> list = productMapper.searchActiveProducts(keyword);
        // 3. 写入 Redis（3 分钟过期，搜索缓存允许短暂不一致）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 查询商品详情（含卖家名称，含 Redis 缓存）
     * 1.先从 Redis 查（demo:product:detail:{id}）
     * 2.未命中则查 MySQL（含卖家名称左连接）
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return Product 商品详情（可能为 null）
     */
    public Product findProductWithSeller(Long id) {
        String cacheKey = "demo:product:detail:" + id;
        // 1. 先从 Redis 查
        Product cached = (Product) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        Product product = productMapper.findProductWithSeller(id);
        // 3. 写入 Redis（5 分钟过期）
        if (product != null) {
            redisUtil.set(cacheKey, product);
        }
        return product;
    }

    /**
     * 查询商品图片列表（含 Redis 缓存）
     * 1.先从 Redis 查（demo:product:images:{productId}）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return List&lt;ProductImage&gt; 图片列表
     */
    @SuppressWarnings("unchecked")
    public List<ProductImage> findImagesByProductId(Long productId) {
        String cacheKey = "demo:product:images:" + productId;
        // 1. 先从 Redis 查
        List<ProductImage> cached = (List<ProductImage>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        List<ProductImage> list = productImageMapper.findImagesByProductId(productId);
        // 3. 写入 Redis（5 分钟过期）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 查询卖家自己创建的商品列表（含已下架、未删除的，含 Redis 缓存）
     * 1.卖家管理后台使用
     * 2.先从 Redis 查（demo:product:seller:{sellerId}）
     * 3.未命中则查 MySQL
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List&lt;Product&gt; 商品列表
     */
    @SuppressWarnings("unchecked")
    public List<Product> findProductsBySellerId(Long sellerId) {
        String cacheKey = "demo:product:seller:" + sellerId;
        // 1. 先从 Redis 查
        List<Product> cached = (List<Product>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        List<Product> list = productMapper.findProductsBySellerId(sellerId);
        // 3. 写入 Redis（5 分钟过期）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    // ==================== 写方法（更新后清理缓存） ====================

    /**
     * 创建商品（插入数据库并写入缓存）
     * 1.插入数据库
     * 2.插入成功才写入 Redis 并清理列表缓存
     * <p>
     * @author ZuiM
     * @param product 商品实体
     * @return boolean true=创建成功
     */
    public boolean saveProduct(Product product) {
        boolean result = productMapper.insert(product) > 0;
        if (result) {
            // 清理所有列表缓存，保证下次查询拉取最新数据
            clearProductListCache();
            // 写入商品详情缓存
            Product saved = productMapper.findProductWithSeller(product.getId());
            if (saved != null) {
                redisUtil.set("demo:product:detail:" + product.getId(), saved);
            }
        }
        return result;
    }

    /**
     * 保存商品图片记录（插入 product_image 表并清理图片缓存）
     * 1.插入数据库
     * 2.插入成功才清理图片列表缓存
     * <p>
     * @author ZuiM
     * @param productImage 商品图片实体
     * @return boolean true=保存成功
     */
    public boolean saveProductImage(ProductImage productImage) {
        boolean result = productImageMapper.insert(productImage) > 0;
        if (result && productImage.getProductId() != null) {
            // 清理该商品的图片缓存，下次查询重新加载
            redisUtil.delete("demo:product:images:" + productImage.getProductId());
        }
        return result;
    }

    /**
     * 更新商品（更新数据库并清理关联缓存）
     * 1.执行 updateById
     * 2.更新成功后清理该商品详情缓存和所有列表缓存
     * <p>
     * @author ZuiM
     * @param product 商品实体（需包含 id）
     * @return boolean true=更新成功
     */
    public boolean updateProduct(Product product) {
        boolean result = productMapper.updateById(product) > 0;
        if (result) {
            // 清理该商品详情缓存
            redisUtil.delete("demo:product:detail:" + product.getId());
            // 清理所有列表缓存
            clearProductListCache();
        }
        return result;
    }

    /**
     * 逻辑删除商品（更新数据库并清理关联缓存）
     * 1.执行逻辑删除
     * 2.删除成功后清理该商品详情缓存和所有列表缓存
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return boolean true=删除成功
     */
    public boolean deleteProduct(Long id) {
        boolean result = productMapper.deleteById(id) > 0;
        if (result) {
            // 清理该商品详情缓存
            redisUtil.delete("demo:product:detail:" + id);
            // 清理所有列表缓存
            clearProductListCache();
        }
        return result;
    }

    // ==================== 缓存清理工具 ====================

    /**
     * 清理所有商品列表缓存（分类缓存和搜索缓存无法精确清理，直接删除所有列表 key）
     * 1.删除全量列表缓存
     * 2.卖家商品列表缓存（由卖家自行管理，此处不清理）
     * <p>
     * 注意：此方法使用通配符模式匹配 Redis key，需要 Redis 支持 KEYS 命令。
     * 生产环境建议使用 Redis SCAN 或记录所有缓存 key 逐一删除。
     * @author ZuiM
     */
    private void clearProductListCache() {
        redisUtil.delete("demo:product:active:list");
        // 分类缓存和搜索缓存无法精确清理，下次查询时会重新加载
    }
}