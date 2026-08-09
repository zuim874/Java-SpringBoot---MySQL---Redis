package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Seller;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卖家数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * <p>
 * @author ZuiM
 */
@Mapper
public interface SellerMapper extends BaseMapper<Seller> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成
}