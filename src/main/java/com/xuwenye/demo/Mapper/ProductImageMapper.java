package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品图片数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按商品ID查询图片列表、主图查询
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 按商品ID查询所有图片（按排序号）
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return List<ProductImage> 图片列表
     */
    @Select("SELECT * FROM product_image WHERE product_id = #{productId} AND is_deleted = 0 ORDER BY sort ASC")
    List<ProductImage> findImagesByProductId(@Param("productId") Long productId);

    /**
     * 按商品ID查询主图
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return ProductImage 主图（可能为null）
     */
    @Select("SELECT * FROM product_image WHERE product_id = #{productId} AND is_main = 1 AND is_deleted = 0")
    ProductImage findMainImageByProductId(@Param("productId") Long productId);
}
