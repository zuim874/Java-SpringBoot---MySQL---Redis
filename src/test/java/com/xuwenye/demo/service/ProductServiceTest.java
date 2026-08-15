package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.ProductService;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.商品 CRUD：新增/查询/上架/下架/删除
 * 2.Redis 缓存：Cache-Aside 模式的列表/分类/详情缓存
 * 3.库存操作：扣减（分布式锁）与恢复，库存不足拦截
 * 4.分页查询：上架商品分页
 * <p>
 * @author ZuiM
 */
class ProductServiceTest extends AbstractServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisUtil redisUtil;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试卖家（唯一名称）
     * <p>
     * @author ZuiM
     * @return Seller 卖家
     */
    private Seller buildSeller() {
        Seller seller = new Seller();
        seller.setSellerName("测试卖家" + SUFFIX);
        seller.setAddress("测试地址" + SUFFIX);
        seller.setSellerContact("13800000000");
        seller.setCreateTime(LocalDateTime.now());
        sellerMapper.insert(seller);
        return seller;
    }

    /**
     * 构造测试商品
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @param stock 库存
     * @return Product 商品
     */
    private Product buildProduct(Long sellerId, int stock) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setProductName("测试商品" + SUFFIX);
        product.setPrice(new BigDecimal("99.90"));
        product.setStock(stock);
        product.setSold(0);
        product.setStatus(1);
        product.setDescription("单元测试商品描述");
        product.setCategory("其他");
        product.setMainImageUrl("http://test.com/p_" + SUFFIX + ".png");
        product.setRecommend(0);
        product.setCreateTime(LocalDateTime.now());
        return product;
    }

    /**
     * 清理商品缓存
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     */
    private void cleanProductCache(Long productId) {
        trackCacheKey("demo:product:detail:" + productId);
        trackCacheKey("demo:product:list:all");
        trackCacheKey("demo:product:categories:all");
        trackCacheKey("demo:product:category:" + "其他");
    }

    // ========== 1. 商品新增与查询 ==========

    /**
     * 新增商品后可按 ID 查询（含 Redis 缓存写入）
     * <p>
     * @author ZuiM
     */
    @Test
    void 新增商品并可查询() {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), 100);
        assertTrue(productService.saveProduct(product));
        assertNotNull(product.getId());

        Product found = productService.getProductById(product.getId());
        assertNotNull(found);
        assertEquals(product.getProductName(), found.getProductName());
        // 详情缓存已写入
        assertTrue(redisUtil.hasKey("demo:product:detail:" + product.getId()));

        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }

    /**
     * 上架商品列表查询（含缓存）
     * <p>
     * @author ZuiM
     */
    @Test
    void 查询上架商品列表() {
        assertNotNull(productService.getAllOnShelfProducts());
        assertNotNull(productService.getAllCategories());
    }

    /**
     * 按分类查询上架商品
     * <p>
     * @author ZuiM
     */
    @Test
    void 按分类查询商品() {
        assertNotNull(productService.getProductsByCategory("其他"));
    }

    // ========== 2. 上架/下架 ==========

    /**
     * 商品下架后状态变更，再上架恢复
     * <p>
     * @author ZuiM
     */
    @Test
    void 下架与上架商品() {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), 10);
        productService.saveProduct(product);

        assertTrue(productService.offShelfProduct(product.getId()));
        // 直接查库，避免命中旧缓存
        assertEquals(0, productMapper.findProductById(product.getId()).getStatus());

        assertTrue(productService.onShelfProduct(product.getId()));
        assertEquals(1, productMapper.findProductById(product.getId()).getStatus());

        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }

    // ========== 3. 库存操作 ==========

    /**
     * 库存扣减：正常扣减成功，库存不足失败
     * <p>
     * @author ZuiM
     */
    @Test
    void 库存扣减与不足拦截() {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), 10);
        productService.saveProduct(product);

        // 正常扣减
        assertTrue(productService.deductStock(product.getId(), 4));
        // 直接查库，避免命中旧缓存
        assertEquals(6, productMapper.findProductById(product.getId()).getStock());

        // 库存不足（还需 7，库存只剩 6）
        assertFalse(productService.deductStock(product.getId(), 7));
        assertEquals(6, productMapper.findProductById(product.getId()).getStock());

        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }

    /**
     * 库存恢复（取消订单/退款场景）
     * <p>
     * @author ZuiM
     */
    @Test
    void 库存恢复() {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), 10);
        productService.saveProduct(product);

        productService.deductStock(product.getId(), 3);
        // 直接查库，避免命中旧缓存
        assertEquals(7, productMapper.findProductById(product.getId()).getStock());

        assertTrue(productService.restoreStock(product.getId(), 3));
        assertEquals(10, productMapper.findProductById(product.getId()).getStock());

        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }

    // ========== 4. 分页查询 ==========

    /**
     * 上架商品分页查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 上架商品分页查询() {
        var page = productService.getOnShelfProductsPage(1, 10, null, null);
        assertNotNull(page);
        assertTrue(page.getRecords().size() <= 10);
    }

    /**
     * 逻辑删除商品后不可查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 删除商品() {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), 10);
        productService.saveProduct(product);

        assertTrue(productService.deleteProduct(product.getId()));
        assertNull(productService.getProductById(product.getId()));

        cleanProductCache(product.getId());
        sellerMapper.deleteById(seller.getId());
    }
}
