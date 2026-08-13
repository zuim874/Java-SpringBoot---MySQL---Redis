package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final MQProducer mqProducer;

    // Redis缓存Key前缀
    private static final String SELLER_LIST_CACHE_KEY = "demo:seller:list:all";
    private static final String SELLER_DETAIL_CACHE_PREFIX = "demo:seller:detail:";

    public SellerService(SellerMapper sellerMapper, RedisUtil redisUtil, MQProducer mqProducer) {
        this.sellerMapper = sellerMapper;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
    }

    /**
     * 分页查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查（仅第一页缓存）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<Seller> 分页卖家列表
     */
    public Page<Seller> getSellerPage(int page, int size) {
        // 使用 MyBatis-Plus 分页查询，排除逻辑删除记录
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<Seller>()
                .eq(Seller::getIsDeleted, 0);
        Page<Seller> sellerPage = sellerMapper.selectPage(new Page<>(page, size), wrapper);
        return sellerPage;
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
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
        }
        return result;
    }

    /**
     * 更新卖家信息
     * 1.执行 updateById
     * 2.清除所有相关缓存（异步 MQ）
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（必须包含id）
     * @return boolean true=更新成功
     */
    @Transactional
    public boolean updateSeller(Seller seller) {
        boolean result = sellerMapper.updateById(seller) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
            // 附带清除详情缓存
            List<String> keys = new ArrayList<>();
            keys.add(SELLER_DETAIL_CACHE_PREFIX + seller.getId());
            mqProducer.sendCacheRefreshTask("seller", "delete", keys);
        }
        return result;
    }

    /**
     * 逻辑删除卖家
     * 1.执行逻辑删除
     * 2.清除所有相关缓存（异步 MQ）
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteSeller(Long id) {
        boolean result = sellerMapper.deleteById(id) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(SELLER_DETAIL_CACHE_PREFIX + id);
            mqProducer.sendCacheRefreshTask("seller", "delete", keys);
        }
        return result;
    }

    /**
     * 按卖家名称查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param sellerName 卖家名称
     * @return Seller 卖家（可能为null）
     */
    public Seller getSellerBySellerName(String sellerName) {
        String cacheKey = SELLER_DETAIL_CACHE_PREFIX + "name:" + sellerName;
        // 第 1 步：先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Seller seller = sellerMapper.findSellerByName(sellerName);
        // 第 3 步：写入 Redis
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }

    /**
     * 发送卖家缓存刷新任务（异步，通过 MQ）
     * 1.清除卖家列表缓存
     * <p>
     * @author ZuiM
     */
    private void sendSellerCacheRefreshTask() {
        List<String> keys = new ArrayList<>();
        keys.add(SELLER_LIST_CACHE_KEY);
        mqProducer.sendCacheRefreshTask("seller", "clear", keys);
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
