package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.OperationLog;
import com.xuwenye.demo.Mapper.OperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志业务层
 * 1.提供保存操作日志的方法（由 ActiveMQ 消费者调用）
 * 2.纯写操作，无需缓存
 * <p>
 * @author ZuiM
 */
@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 保存操作日志
     * 1.由 ActiveMQ 消费者异步调用
     * 2.直接写入数据库，无缓存逻辑
     * <p>
     * @author ZuiM
     * @param log 操作日志实体
     */
    @Transactional
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }
}