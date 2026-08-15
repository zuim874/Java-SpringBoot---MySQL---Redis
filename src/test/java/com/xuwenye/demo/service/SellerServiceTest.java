package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 卖家业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.卖家 CRUD：新增/查询/更新/逻辑删除
 * 2.Redis 缓存：Cache-Aside 模式的详情/按名/列表缓存
 * 3.分页查询：卖家分页
 * <p>
 * @author ZuiM
 */
class SellerServiceTest extends AbstractServiceTest {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private SellerMapper sellerMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试卖家（名称唯一）
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
        return seller;
    }

    /**
     * 清理测试卖家（逻辑删除 + 清理缓存）
     * <p>
     * @author ZuiM
     * @param seller 卖家
     */
    private void cleanSeller(Seller seller) {
        if (seller != null && seller.getId() != null) {
            trackCacheKey("demo:seller:detail:" + seller.getId());
            trackCacheKey("demo:seller:detail:name:" + seller.getSellerName());
            trackCacheKey("demo:seller:list:all");
            sellerMapper.deleteById(seller.getId());
        }
    }

    // ========== 1. 新增与查询 ==========

    /**
     * 新增卖家后可按 ID 查询（含 Redis 缓存写入）
     * <p>
     * @author ZuiM
     */
    @Test
    void 新增卖家并可按id查询() {
        Seller seller = buildSeller();
        assertTrue(sellerService.saveSeller(seller));
        assertNotNull(seller.getId());

        Seller found = sellerService.getSellerById(seller.getId());
        assertNotNull(found);
        assertEquals(seller.getSellerName(), found.getSellerName());
        // 详情缓存已写入
        assertTrue(redisUtil.hasKey("demo:seller:detail:" + seller.getId()));

        cleanSeller(seller);
    }

    /**
     * 按卖家名称查询（含 Redis 缓存写入）
     * <p>
     * @author ZuiM
     */
    @Test
    void 按名称查询卖家() {
        Seller seller = buildSeller();
        sellerService.saveSeller(seller);

        Seller found = sellerService.getSellerBySellerName(seller.getSellerName());
        assertNotNull(found);
        assertEquals(seller.getId(), found.getId());

        cleanSeller(seller);
    }

    // ========== 2. 更新 ==========

    /**
     * 更新卖家信息（联系方式变更后库中生效）
     * <p>
     * @author ZuiM
     */
    @Test
    void 更新卖家信息() {
        Seller seller = buildSeller();
        sellerService.saveSeller(seller);

        seller.setSellerContact("13900000000");
        assertTrue(sellerService.updateSeller(seller));
        assertEquals("13900000000", sellerMapper.selectById(seller.getId()).getSellerContact());

        cleanSeller(seller);
    }

    // ========== 3. 删除 ==========

    /**
     * 逻辑删除卖家后不可查询（is_deleted=1 被过滤）
     * <p>
     * @author ZuiM
     */
    @Test
    void 逻辑删除卖家() {
        Seller seller = buildSeller();
        sellerService.saveSeller(seller);

        assertTrue(sellerService.deleteSeller(seller.getId()));
        assertNull(sellerService.getSellerById(seller.getId()));

        cleanSeller(seller);
    }

    // ========== 4. 列表与分页 ==========

    /**
     * 卖家列表与分页查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 卖家列表与分页查询() {
        assertNotNull(sellerService.getAllSellers());
        trackCacheKey("demo:seller:list:all");

        var page = sellerService.getSellerPage(1, 10);
        assertNotNull(page);
        assertTrue(page.getRecords().size() <= 10);
    }
}
