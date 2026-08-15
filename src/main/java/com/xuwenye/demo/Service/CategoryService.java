package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.Category;
import com.xuwenye.demo.Mapper.CategoryMapper;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类业务层（管理员维护的预设分类）
 * 1.查询：启用分类列表，带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/删除分类，写库后通过 MQ 异步清理缓存
 * 3.名称↔ID 翻译：商品表 category 列存分类ID集合，前端展示需转为名称
 * <p>
 * 说明：分类仅允许管理员添加/删除，卖家只能从已有分类中选取（不可自编辑）
 * <p>
 * @author ZuiM
 */
@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final RedisUtil redisUtil;
    private final MQProducer mqProducer;

    /** 启用分类列表缓存 key */
    private static final String CATEGORY_CACHE_KEY = "demo:category:list";
    /** 商品分类名称缓存 key（由 ProductService 维护，新增/删除分类时一并清理） */
    private static final String PRODUCT_CATEGORIES_CACHE_KEY = "demo:product:categories:all";

    public CategoryService(CategoryMapper categoryMapper,
                           ProductMapper productMapper,
                           RedisUtil redisUtil,
                           MQProducer mqProducer) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
    }

    /**
     * 获取所有启用分类（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List<Category> 启用分类列表（含 id/name/sort）
     */
    @SuppressWarnings("unchecked")
    public List<Category> getAllCategories() {
        // 第 1 步：先从 Redis 查
        List<Category> cached = (List<Category>) redisUtil.get(CATEGORY_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Category> categories = categoryMapper.findEnabledCategories();
        // 第 3 步：写入 Redis
        if (categories != null && !categories.isEmpty()) {
            redisUtil.set(CATEGORY_CACHE_KEY, categories);
        }
        return categories == null ? new ArrayList<>() : categories;
    }

    /**
     * 按名称查询分类ID（用于商品按名称筛选时翻译为ID）
     * 不缓存：该场景查询频率低且依赖分类实时性
     * <p>
     * @author ZuiM
     * @param name 分类名称
     * @return Long 分类ID（不存在返回 null）
     */
    public Long getCategoryIdByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Category category = categoryMapper.findByName(name.trim());
        return category == null ? null : category.getId();
    }

    /**
     * 新增分类（仅管理员）
     * 1.校验分类名称非空
     * 2.校验名称唯一
     * 3.插入记录
     * 4.异步清理分类缓存（MQ）
     * <p>
     * @author ZuiM
     * @param name 分类名称
     * @param sort 排序号（默认0）
     * @return boolean true=新增成功
     */
    @Transactional
    public boolean addCategory(String name, Integer sort) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("分类名称不能超过50个字符");
        }
        if (categoryMapper.findByName(trimmed) != null) {
            throw new IllegalArgumentException("分类「" + trimmed + "」已存在");
        }

        Category category = new Category();
        category.setName(trimmed);
        category.setSort(sort == null ? 0 : sort);
        category.setStatus(1);
        boolean result = categoryMapper.insert(category) > 0;
        if (result) {
            sendCategoryCacheRefreshTask();
        }
        return result;
    }

    /**
     * 删除分类（仅管理员）
     * 1.校验分类存在
     * 2.校验无商品引用该分类（避免商品分类数据悬空）
     * 3.物理删除
     * 4.异步清理分类与商品缓存（MQ）
     * <p>
     * @author ZuiM
     * @param id 分类ID
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        // 检查是否有商品引用该分类（商品的 category 列以英文逗号分隔存储分类ID集合）
        Long usedCount = productMapper.countByCategoryId(id);
        if (usedCount != null && usedCount > 0) {
            throw new IllegalArgumentException("该分类已被 " + usedCount + " 个商品使用，无法删除");
        }
        boolean result = categoryMapper.deleteById(id) > 0;
        if (result) {
            sendCategoryCacheRefreshTask();
        }
        return result;
    }

    /**
     * 发送分类缓存刷新任务（异步，通过 MQ）
     * 1.清理启用分类列表缓存
     * 2.清理商品分类名称缓存
     * 3.清理按分类查询的商品缓存（模糊匹配）
     * <p>
     * @author ZuiM
     */
    private void sendCategoryCacheRefreshTask() {
        List<String> keys = new ArrayList<>();
        keys.add(CATEGORY_CACHE_KEY);
        keys.add(PRODUCT_CATEGORIES_CACHE_KEY);
        keys.add("demo:product:category:*");
        keys.add("demo:product:page:*");
        keys.add("demo:product:admin:page:*");
        keys.add("demo:product:seller:*");
        mqProducer.sendCacheRefreshTask("category", "clear", keys);
    }
}
