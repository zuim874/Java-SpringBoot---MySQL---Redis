package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品分类数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：启用分类列表、按名称查询
 * <p>
 * @author ZuiM
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 查询所有启用的分类（按排序号、ID 升序）
     * <p>
     * @author ZuiM
     * @return List<Category> 分类列表
     */
    @Select("SELECT * FROM sys_category WHERE status = 1 ORDER BY sort ASC, id ASC")
    List<Category> findEnabledCategories();

    /**
     * 按名称查询分类（唯一）
     * <p>
     * @author ZuiM
     * @param name 分类名称
     * @return Category 分类（可能为null）
     */
    @Select("SELECT * FROM sys_category WHERE name = #{name} LIMIT 1")
    Category findByName(@Param("name") String name);
}
