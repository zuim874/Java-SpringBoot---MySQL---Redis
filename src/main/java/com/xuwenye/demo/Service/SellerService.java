package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 卖家业务层
 * 1.查询：卖家列表/详情，均带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/更新/删除，写库后维护缓存
 * <p>
 * @author ZuiM
 */
@Service
public class SellerService {
    private final SellerMapper sellerMapper;
    private final RedisUtil redisUtil;

    // Redis缓存Key前缀
    private static final String SELLER_LIST_CACHE_KEY = "demo:seller:list:all";
    private static final String SELLER_DETAIL_CACHE_PREFIX = "demo:seller:detail:";

    public SellerService(SellerMapper sellerMapper, RedisUtil redisUtil) {
        this.sellerMapper = sellerMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 获取所有卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List<Seller> 卖家列表
     */
    @SuppressWarnings("unchecked")
    public List<Seller> getAllSellers() {
        // 第 1 步：先从 Redis 查
        List<Seller> cached = (List<Seller>) redisUtil.get(SELLER_LIST_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Seller> sellers = sellerMapper.findAllSellers();
        // 第 3 步：写入 Redis
        if (sellers != null && !sellers.isEmpty()) {
            redisUtil.set(SELLER_LIST_CACHE_KEY, sellers);
        }
        return sellers;
    }

    /**
     * 按ID查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return Seller 卖家（可能为null）
     */
    public Seller getSellerById(Long id) {
        String cacheKey = SELLER_DETAIL_CACHE_PREFIX + id;
        // 第 1 步：先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Seller seller = sellerMapper.findSellerById(id);
        // 第 3 步：写入 Redis
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }

    /**
     * 保存卖家（新增）
     * 1.插入数据库
     * 2.清除卖家列表缓存
     * <p>
     * @author ZuiM
     * @param seller 卖家实体
     * @return boolean true=保存成功
     */
    @Transactional
    public boolean saveSeller(Seller seller) {
        boolean result = sellerMapper.insert(seller) > 0;
        if (result) {
            // 清除列表缓存
            redisUtil.delete(SELLER_LIST_CACHE_KEY);
        }
        return result;
    }

    /**
     * 更新卖家信息
     * 1.执行 updateById
     * 2.清除所有相关缓存
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（必须包含id）
     * @return boolean true=更新成功
     */
    @Transactional
    public boolean updateSeller(Seller seller) {
        boolean result = sellerMapper.updateById(seller) > 0;
        if (result) {
            // 清除详情缓存
            redisUtil.delete(SELLER_DETAIL_CACHE_PREFIX + seller.getId());
            // 清除列表缓存
            redisUtil.delete(SELLER_LIST_CACHE_KEY);
            // 重新加载详情缓存
            reloadSellerDetail(seller.getId());
        }
        return result;
    }

    /**
     * 逻辑删除卖家
     * 1.执行逻辑删除
     * 2.清除所有相关缓存
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteSeller(Long id) {
        Seller seller = sellerMapper.findSellerById(id);
        boolean result = sellerMapper.deleteById(id) > 0;
        if (result && seller != null) {
            // 清除详情缓存
            redisUtil.delete(SELLER_DETAIL_CACHE_PREFIX + id);
            // 清除列表缓存
            redisUtil.delete(SELLER_LIST_CACHE_KEY);
        }
        return result;
    }

    /**
     * 重新加载卖家详情缓存
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     */
    private void reloadSellerDetail(Long id) {
        Seller seller = sellerMapper.findSellerById(id);
        if (seller != null) {
            redisUtil.set(SELLER_DETAIL_CACHE_PREFIX + id, seller);
        }
    }
}
