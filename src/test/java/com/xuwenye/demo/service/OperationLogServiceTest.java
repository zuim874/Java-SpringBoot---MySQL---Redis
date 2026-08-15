package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.OperationLog;
import com.xuwenye.demo.Mapper.OperationLogMapper;
import com.xuwenye.demo.Service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作日志业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.成功日志：saveLog 落库并生成自增 ID
 * 2.失败日志：错误信息正确入库
 * <p>
 * @author ZuiM
 */
class OperationLogServiceTest extends AbstractServiceTest {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private OperationLogMapper operationLogMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造操作日志实体
     * <p>
     * @author ZuiM
     * @param status 操作状态 0失败 1成功
     * @return OperationLog 日志
     */
    private OperationLog buildLog(int status) {
        OperationLog log = new OperationLog();
        log.setUsername("tester_" + SUFFIX);
        log.setOperation("单元测试操作");
        log.setMethod("TestController.testMethod");
        log.setRequestParams("{\"page\":1}");
        log.setRequestUrl("/api/test");
        log.setRequestIp("127.0.0.1");
        log.setStatus(status);
        log.setDuration(15L);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    // ========== 1. 成功日志 ==========

    /**
     * 保存成功日志后数据完整落库
     * <p>
     * @author ZuiM
     */
    @Test
    void 保存成功日志落库() {
        OperationLog log = buildLog(1);
        operationLogService.saveLog(log);
        assertNotNull(log.getId());

        OperationLog reloaded = operationLogMapper.selectById(log.getId());
        assertEquals(1, reloaded.getStatus());
        assertEquals("/api/test", reloaded.getRequestUrl());
        assertEquals("TestController.testMethod", reloaded.getMethod());
        assertEquals(15L, reloaded.getDuration());

        operationLogMapper.deleteById(log.getId());
    }

    // ========== 2. 失败日志 ==========

    /**
     * 保存失败日志时错误信息正确入库
     * <p>
     * @author ZuiM
     */
    @Test
    void 保存失败日志记录错误信息() {
        OperationLog log = buildLog(0);
        log.setErrorMsg("账号或密码错误");
        operationLogService.saveLog(log);
        assertNotNull(log.getId());

        OperationLog reloaded = operationLogMapper.selectById(log.getId());
        assertEquals(0, reloaded.getStatus());
        assertEquals("账号或密码错误", reloaded.getErrorMsg());

        operationLogMapper.deleteById(log.getId());
    }

    /**
     * 未登录场景（userId 为 null）也可正常记录
     * <p>
     * @author ZuiM
     */
    @Test
    void 无用户日志也可记录() {
        OperationLog log = buildLog(1);
        log.setUserId(null);
        log.setUsername("anonymous");
        operationLogService.saveLog(log);
        assertNotNull(log.getId());

        OperationLog reloaded = operationLogMapper.selectById(log.getId());
        assertNull(reloaded.getUserId());
        assertEquals("anonymous", reloaded.getUsername());

        operationLogMapper.deleteById(log.getId());
    }
}
