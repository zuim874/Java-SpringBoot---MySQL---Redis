package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按分类查询、按卖家查询、查询上架商品
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 查询所有上架商品（状态为1）
     * <p>
     * @author ZuiM
     * @return List<Product> 上架商品列表
     */
    @Select("SELECT * FROM sys_product WHERE status = 1 AND is_deleted = 0")
    List<Product> findAllOnShelfProducts();

    /**
     * 按分类查询上架商品
     * <p>
     * @author ZuiM
     * @param category 商品分类
     * @return List<Product> 指定分类的上架商品列表
     */
    @Select("SELECT * FROM sys_product WHERE category = #{category} AND status = 1 AND is_deleted = 0")
    List<Product> findProductsByCategory(@Param("category") String category);

    /**
     * 按卖家ID查询上架商品
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List<Product> 该卖家的上架商品列表
     */
    @Select("SELECT * FROM sys_product WHERE seller_id = #{sellerId} AND is_deleted = 0")
    List<Product> findProductsBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 按ID查询商品（包含已下架）
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return Product 商品（可能为null）
     */
    @Select("SELECT * FROM sys_product WHERE id = #{id} AND is_deleted = 0")
    Product findProductById(@Param("id") Long id);

    /**
     * 查询所有分类（不重复）
     * <p>
     * @author ZuiM
     * @return List<String> 分类列表
     */
    @Select("SELECT DISTINCT category FROM sys_product WHERE is_deleted = 0 AND category IS NOT NULL")
    List<String> findAllCategories();

    // ======================== 分页查询方法 ========================

    /**
     * 分页查询上架商品（支持按分类筛选）
     * <p>
     * @author ZuiM
     * @param page 分页参数
     * @param category 商品分类（为null则查询全部）
     * @return Page<Product> 分页商品列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_product WHERE status = 1 AND is_deleted = 0" +
            "<if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (product_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY recommend DESC, create_time DESC" +
            "</script>")
    Page<Product> selectOnShelfProductsPage(Page<Product> page,
                                           @Param("category") String category,
                                           @Param("keyword") String keyword);

    /**
     * 分页查询所有商品（包含已下架，管理员用）
     * <p>
     * @author ZuiM
     * @param page 分页参数
     * @return Page<Product> 分页商品列表
     */
    @Select("SELECT * FROM sys_product WHERE is_deleted = 0 ORDER BY create_time DESC")
    Page<Product> selectAllProductsPage(Page<Product> page);

    /**
     * 分页查询卖家自己的商品（包含已下架）
     * <p>
     * @author ZuiM
     * @param page 分页参数
     * @param sellerId 卖家ID
     * @return Page<Product> 分页商品列表
     */
    @Select("SELECT * FROM sys_product WHERE seller_id = #{sellerId} AND is_deleted = 0 ORDER BY create_time DESC")
    Page<Product> selectProductsBySellerIdPage(Page<Product> page, @Param("sellerId") Long sellerId);
}
