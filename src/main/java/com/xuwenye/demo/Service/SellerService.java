package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;

/**
 * 卖家业务层
 * 1.查询卖家信息（含 Redis 缓存）
 * 2.按卖家名称查询（用于登录/卖家身份校验）
 * <p>
 * @author ZuiM
 */
@Service
public class SellerService {
    private final SellerMapper sellerMapper;
    private final RedisUtil redisUtil;

    public SellerService(SellerMapper sellerMapper,
                         RedisUtil redisUtil) {
        this.sellerMapper = sellerMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 根据卖家 ID 查询（含 Redis 缓存）
     * 1.先从 Redis 查（demo:seller:id:{id}）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 卖家 ID
     * @return Seller 卖家（可能为 null）
     */
    public Seller getSellerById(Long id) {
        String cacheKey = "demo:seller:id:" + id;
        // 1. 先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. Redis 没有，查 MySQL
        Seller seller = sellerMapper.selectById(id);
        // 3. 写入 Redis（10 分钟过期）
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }
}