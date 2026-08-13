package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.RechargeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 充值申请数据访问层
 * <p>
 * @author ZuiM
 */
@Mapper
public interface RechargeRequestMapper extends BaseMapper<RechargeRequest> {

    /**
     * 按用户ID查询充值申请记录（按时间倒序）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<RechargeRequest>
     */
    @Select("SELECT * FROM sys_recharge_request WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<RechargeRequest> findByUserId(@Param("userId") Long userId);

    /**
     * 分页查询所有充值申请（按时间倒序，管理员用）
     * <p>
     * @author ZuiM
     * @param page 分页参数
     * @return IPage<RechargeRequest>
     */
    @Select("SELECT * FROM sys_recharge_request ORDER BY create_time DESC")
    IPage<RechargeRequest> findPage(Page<?> page);
}