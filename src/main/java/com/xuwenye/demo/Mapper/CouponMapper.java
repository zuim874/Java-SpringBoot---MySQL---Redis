package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 优惠券模板数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义操作：原子扣减剩余发放数量（并发安全，防止超发）
 * 3.领券中心：查询可自助领取的模板
 * <p>
 * @author ZuiM
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子扣减优惠券剩余数量
     * 仅当 remain_count > 0 时扣减成功，防止并发下超发
     * <p>
     * @author ZuiM
     * @param id 优惠券ID
     * @return int 受影响行数（0=无可发数量）
     */
    @Update("UPDATE sys_coupon SET remain_count = remain_count - 1 " +
            "WHERE id = #{id} AND remain_count > 0")
    int decrementRemain(@Param("id") Long id);

    /**
     * 归还优惠券剩余数量（发放失败回滚时调用）
     * 1.用于唯一索引兜底命中后的库存回滚，保证不超发也不漏发
     * <p>
     * @author ZuiM
     * @param id 优惠券ID
     * @return int 受影响行数
     */
    @Update("UPDATE sys_coupon SET remain_count = remain_count + 1 WHERE id = #{id}")
    int incrementRemain(@Param("id") Long id);

    /**
     * 查询可自助领取的优惠券模板（启用中且仍有剩余数量，按创建时间倒序）
     * 供首页「领券中心」使用
     * <p>
     * @author ZuiM
     * @return List<Coupon> 可领取的模板列表
     */
    @Select("SELECT * FROM sys_coupon WHERE status = 1 AND remain_count > 0 ORDER BY create_time DESC")
    List<Coupon> selectClaimableTemplates();
}
