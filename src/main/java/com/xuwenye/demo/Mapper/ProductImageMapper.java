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
 * 2.自定义查询：按商品ID查询图片列表
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 按商品ID查询所有未删除的图片（按排序号升序）
     * 1.只查询未删除的图片（is_deleted = 0）
     * 2.按 sort 升序排列，主图（is_main=1）排在最前
     * <p>
     * @author ZuiM
     * @param productId 商品ID
     * @return List&lt;ProductImage&gt; 图片列表
     */
    @Select("SELECT * FROM product_image WHERE product_id = #{productId} AND is_deleted = 0 ORDER BY sort ASC")
    List<ProductImage> findImagesByProductId(@Param("productId") Long productId);
}