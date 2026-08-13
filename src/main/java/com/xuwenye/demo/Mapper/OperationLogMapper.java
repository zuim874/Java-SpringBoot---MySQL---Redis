package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.操作日志由 ActiveMQ 消费者异步写入，无需自定义查询
 * <p>
 * @author ZuiM
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成
}