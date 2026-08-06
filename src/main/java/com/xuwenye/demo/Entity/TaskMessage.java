package com.xuwenye.demo.Entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TaskMessage implements Serializable {
    private String taskId;
    private String taskType;
    private String businessType;
    private Map<String, Object> data;
    private Long userId;
    private String operator;
    private LocalDateTime createTime;
    private Integer priority = 1;
    private Integer retryCount = 0;
}
