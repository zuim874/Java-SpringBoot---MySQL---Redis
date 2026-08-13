package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（对应 sys_operation_log 表）
 * 1.记录用户的关键操作行为，如登录、注册、修改密码、充值、下单等
 * 2.通过 ActiveMQ 异步写入，不阻塞主业务流程
 * 3.status 字段标记操作成功/失败
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID（可能为null，如未登录的注册操作） */
    @TableField("user_id")
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 操作名称（如：用户登录、用户注册、修改密码、用户充值） */
    private String operation;

    /** 请求方法（Controller类名.方法名） */
    private String method;

    /** 请求参数（JSON格式） */
    @TableField("request_params")
    private String requestParams;

    /** 请求URL */
    @TableField("request_url")
    private String requestUrl;

    /** 请求IP地址 */
    @TableField("request_ip")
    private String requestIp;

    /** 操作状态：0失败 1成功 */
    private Integer status;

    /** 错误信息（失败时记录） */
    @TableField("error_msg")
    private String errorMsg;

    /** 请求耗时（毫秒） */
    private Long duration;

    /** 操作时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}