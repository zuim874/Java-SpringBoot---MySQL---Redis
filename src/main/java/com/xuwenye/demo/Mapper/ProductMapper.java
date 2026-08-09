package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按分类查询、按名称模糊搜索、查询已上架商品
 * 3.自定义更新：更新销量、更新库存
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 查询所有已上架且未删除的商品（含卖家名称，通过左连接）
     * 1.只查询 status=1 且 is_deleted=0 的商品
     * 2.左连接 sys_seller 获取卖家名称
     * 3.按创建时间降序排列
     * <p>
     * @author ZuiM
     * @return List&lt;Product&gt; 商品列表（含卖家名称）
     */
    @Select("SELECT p.*, s.seller_name AS sellerName " +
            "FROM sys_product p " +
            "LEFT JOIN sys_seller s ON p.seller_id = s.id " +
            "WHERE p.status = 1 AND p.is_deleted = 0 " +
            "ORDER BY p.create_time DESC")
    List<Product> findActiveProducts();

    /**
     * 按分类查询已上架商品（含卖家名称）
     * 1.只查询 status=1 且 is_deleted=0 的商品
     * 2.按分类精确匹配
     * 3.含卖家名称
     * <p>
     * @author ZuiM
     * @param category 商品分类
     * @return List&lt;Product&gt; 商品列表
     */
    @Select("SELECT p.*, s.seller_name AS sellerName " +
            "FROM sys_product p " +
            "LEFT JOIN sys_seller s ON p.seller_id = s.id " +
            "WHERE p.status = 1 AND p.is_deleted = 0 AND p.category = #{category} " +
            "ORDER BY p.create_time DESC")
    List<Product> findActiveProductsByCategory(@Param("category") String category);

    /**
     * 按商品名称模糊搜索已上架商品
     * 1.使用 LIKE 模糊匹配
     * 2.只查询已上架未删除的商品
     * <p>
     * @author ZuiM
     * @param keyword 搜索关键词
     * @return List&lt;Product&gt; 商品列表
     */
    @Select("SELECT p.*, s.seller_name AS sellerName " +
            "FROM sys_product p " +
            "LEFT JOIN sys_seller s ON p.seller_id = s.id " +
            "WHERE p.status = 1 AND p.is_deleted = 0 AND p.product_name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY p.create_time DESC")
    List<Product> searchActiveProducts(@Param("keyword") String keyword);

    /**
     * 查询商品详情（含卖家名称）
     * 1.不区分 status（已下架也能查到，前端可显示"已下架"）
     * 2.含卖家名称，用于详情页展示
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @return Product 商品（含卖家名称，可能为 null）
     */
    @Select("SELECT p.*, s.seller_name AS sellerName " +
            "FROM sys_product p " +
            "LEFT JOIN sys_seller s ON p.seller_id = s.id " +
            "WHERE p.id = #{id} AND p.is_deleted = 0")
    Product findProductWithSeller(@Param("id") Long id);

    /**
     * 查询卖家自己创建的商品列表（含已下架、未删除的）
     * 1.卖家管理后台使用，可以查看自己所有商品
     * 2.不限制 status，已下架的也能看到
     * 3.按创建时间降序排列
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List&lt;Product&gt; 商品列表
     */
    @Select("SELECT * FROM sys_product WHERE seller_id = #{sellerId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Product> findProductsBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 增加商品销量
     * 1.原子操作：sold = sold + #{quantity}
     * 2.用于下单后更新销量
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @param quantity 增加数量
     * @return int 受影响行数
     */
    @Update("UPDATE sys_product SET sold = sold + #{quantity} WHERE id = #{id}")
    int increaseSold(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 减少商品库存
     * 1.原子操作：stock = stock - #{quantity}
     * 2.条件：stock >= #{quantity}（防止超卖，由业务层控制）
     * <p>
     * @author ZuiM
     * @param id 商品ID
     * @param quantity 减少数量
     * @return int 受影响行数
     */
    @Update("UPDATE sys_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}