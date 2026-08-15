package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存扣减并发性能测试（需 MySQL + Redis + ActiveMQ）
 * 1.高并发扣减：N 个线程同时扣 1 件，验证恰好成功 N 次（分布式锁串行化）
 * 2.超卖防护：并发请求超过库存时，成功次数 = 库存数，最终库存为 0
 * 3.性能指标：输出总耗时与平均单次扣减耗时
 * <p>
 * @author ZuiM
 */
class StockConcurrencyPerformanceTest extends AbstractServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ProductMapper productMapper;

    /** 并发扣减线程数（也作为初始库存数） */
    private static final int THREAD_COUNT = 50;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试卖家
     * <p>
     * @author ZuiM
     * @return Seller 卖家
     */
    private Seller buildSeller() {
        Seller seller = new Seller();
        seller.setSellerName("并发测试卖家" + SUFFIX);
        seller.setAddress("并发测试地址" + SUFFIX);
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
     * @param stock 初始库存
     * @return Product 商品
     */
    private Product buildProduct(Long sellerId, int stock) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setProductName("并发测试商品" + SUFFIX);
        product.setPrice(new BigDecimal("9.90"));
        product.setStock(stock);
        product.setSold(0);
        product.setStatus(1);
        product.setDescription("并发性能测试商品");
        product.setCategory("其他");
        product.setMainImageUrl("http://test.com/con_" + SUFFIX + ".png");
        product.setRecommend(0);
        product.setCreateTime(LocalDateTime.now());
        return product;
    }

    /**
     * 清理商品关联缓存
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

    // ========== 1. 高并发扣减 ==========

    /**
     * N 个线程并发各扣 1 件：恰好成功 N 次，库存归零，无超卖无丢失
     * <p>
     * @author ZuiM
     */
    @Test
    void 高并发扣减无超卖() throws InterruptedException {
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), THREAD_COUNT);
        productService.saveProduct(product);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 所有线程就绪后同时开跑
                    if (productService.deductStock(product.getId(), 1)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        // 直接查库，避免命中缓存读到旧库存
        Product finalProduct = productMapper.findProductById(product.getId());

        System.out.println("========== 高并发扣减结果 ==========");
        System.out.println("并发线程数: " + THREAD_COUNT);
        System.out.println("是否完成: " + (completed ? "是" : "否（超时）"));
        System.out.println("成功扣减次数: " + successCount.get());
        System.out.println("最终库存: " + finalProduct.getStock());
        System.out.println("总耗时: " + duration + "ms, 平均单次: " + String.format("%.2f", duration * 1.0 / THREAD_COUNT) + "ms");

        // 恰好成功 THREAD_COUNT 次，库存归零（无超卖、无丢失）
        assertEquals(THREAD_COUNT, successCount.get());
        assertEquals(0, finalProduct.getStock());

        productService.restoreStock(product.getId(), THREAD_COUNT);
        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }

    // ========== 2. 超卖防护 ==========

    /**
     * 并发请求（50）超过库存（20）：成功 20 次，库存归零，绝不超卖
     * <p>
     * @author ZuiM
     */
    @Test
    void 并发扣减超过库存时防止超卖() throws InterruptedException {
        int stock = 20;
        int threads = 50;
        Seller seller = buildSeller();
        Product product = buildProduct(seller.getId(), stock);
        productService.saveProduct(product);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (productService.deductStock(product.getId(), 1)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        Product finalProduct = productMapper.findProductById(product.getId());

        System.out.println("========== 超卖防护结果 ==========");
        System.out.println("初始库存: " + stock + ", 并发请求: " + threads);
        System.out.println("成功扣减次数: " + successCount.get());
        System.out.println("最终库存: " + finalProduct.getStock());

        // 成功次数等于库存数，最终库存 0（不会扣出负数）
        assertEquals(stock, successCount.get());
        assertEquals(0, finalProduct.getStock());

        productService.restoreStock(product.getId(), stock);
        cleanProductCache(product.getId());
        productService.deleteProduct(product.getId());
        sellerMapper.deleteById(seller.getId());
    }
}