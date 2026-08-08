package com.xuwenye.demo.Entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息队列任务实体（ActiveMQ 消息体）
 * 1.taskId：任务唯一 ID（UUID）
 * 2.taskType：任务类型（EMAIL/SMS/LOG/ORDER/STATISTICS/FILE/NOTIFICATION）
 * 3.businessType：业务类型（如 VERIFY）
 * 4.data：业务数据（Map 灵活承载）
 * <p>
 * @author ZuiM
 */
@Data
public class TaskMessage implements Serializable {
    /** 任务唯一 ID */
    private String taskId;
    /** 任务类型（EMAIL/SMS/LOG/ORDER 等） */
    private String taskType;
    /** 业务类型（如 VERIFY 验证码） */
    private String businessType;
    /** 业务数据 */
    private Map<String, Object> data;
    /** 关联用户 ID */
    private Long userId;
    /** 操作人 */
    private String operator;
    /** 任务创建时间 */
    private LocalDateTime createTime;
    /** 优先级（默认 1） */
    private Integer priority = 1;
    /** 重试次数（默认 0） */
    private Integer retryCount = 0;
}
