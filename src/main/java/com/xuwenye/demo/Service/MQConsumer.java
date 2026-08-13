package com.xuwenye.demo.Service;

import com.xuwenye.demo.config.activeMQ.ActiveMQConfig;
import com.xuwenye.demo.Entity.OperationLog;
import com.xuwenye.demo.Entity.TaskMessage;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息队列消费者
 * 1.监听各业务队列/主题，消费 TaskMessage
 * 2.邮件任务：调用 EmailUtil 发送验证码邮件，失败进入重试逻辑
 * 3.统计/文件/通知/广播/告警：预留 TODO 处理
 * <p>
 * @author ZuiM
 */
@Slf4j
@Component
public class MQConsumer {
    private final EmailUtil emailUtil;
    private final OperationLogService operationLogService;
    private final RedisUtil redisUtil;
    public MQConsumer(EmailUtil emailUtil, OperationLogService operationLogService, RedisUtil redisUtil) {
        this.emailUtil = emailUtil;
        this.operationLogService = operationLogService;
        this.redisUtil = redisUtil;
    }

    // ========== 1. 消费邮件任务 ==========
    /**
     * 消费邮件任务
     * 1.解析邮件数据（收件人 + 邮件类型编码）
     * 2.调用 EmailUtil 发送验证码邮件
     * 3.失败进入重试逻辑
     * <p>
     * @author ZuiM
     * @param message 邮件任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_EMAIL)
    public void handleEmail(TaskMessage message) {
        try {
            log.info("📧 处理邮件任务: {}", message.getTaskId());
            Map<String, Object> data = message.getData();
            String toEmail = (String) data.get("toEmail");
            Integer emailTypeCode = (Integer) data.get("emailTypeCode");

            emailUtil.sendVerificationCode(toEmail, emailTypeCode);
            log.info("✅ 邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败: {}", e.getMessage());
            // 重试逻辑
            handleRetry(message);
        }
    }

    // ========== 2. 消费短信任务 ==========
//    @JmsListener(destination = ActiveMQConfig.QUEUE_SMS)
//    public void handleSms(TaskMessage message) {
//        try {
//            log.info("📱 处理短信任务: {}", message.getTaskId());
//            Map<String, Object> data = message.getData();
//            String phone = (String) data.get("phone");
//            String code = (String) data.get("code");
//
//            smsService.sendVerificationCode(phone, code);
//            log.info("✅ 短信发送成功: {}", phone);
//        } catch (Exception e) {
//            log.error("❌ 短信发送失败: {}", e.getMessage());
//            handleRetry(message);
//        }
//    }

    // ========== 3. 消费日志任务 ==========
    /**
     * 消费日志任务
     * 1.从 AOP 切面发送的日志数据中提取字段
     * 2.组装 OperationLog 实体
     * 3.异步写入数据库
     * <p>
     * @author ZuiM
     * @param message 日志任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_LOG)
    public void handleLog(TaskMessage message) {
        try {
            log.info("📝 处理日志任务: {}", message.getTaskId());
            Map<String, Object> data = message.getData();

            OperationLog logRecord = new OperationLog();

            // 用户信息
            Object userIdObj = data.get("userId");
            if (userIdObj instanceof Number) {
                logRecord.setUserId(((Number) userIdObj).longValue());
            }
            logRecord.setUsername((String) data.getOrDefault("username", ""));

            // 操作信息
            logRecord.setOperation((String) data.getOrDefault("operation", ""));
            logRecord.setMethod((String) data.getOrDefault("method", ""));
            logRecord.setRequestUrl((String) data.getOrDefault("requestUrl", ""));
            logRecord.setRequestIp((String) data.getOrDefault("requestIp", ""));
            logRecord.setRequestParams((String) data.getOrDefault("requestParams", ""));

            // 状态与耗时
            Object statusObj = data.get("status");
            logRecord.setStatus(statusObj instanceof Number ? ((Number) statusObj).intValue() : 1);
            logRecord.setErrorMsg((String) data.getOrDefault("errorMsg", null));

            Object durationObj = data.get("duration");
            if (durationObj instanceof Number) {
                logRecord.setDuration(((Number) durationObj).longValue());
            }

            // 操作时间（使用消息创建时间）
            logRecord.setCreateTime(LocalDateTime.now());

            // 写入数据库
            operationLogService.saveLog(logRecord);
            log.info("✅ 日志保存成功: {} -> {}", logRecord.getUsername(), logRecord.getOperation());
        } catch (Exception e) {
            log.error("❌ 日志保存失败: {}", e.getMessage(), e);
        }
    }

    // ========== 4. 消费订单任务 ==========
    /**
     * 消费订单任务
     * 1.解析订单操作类型（PROCESS/PAYMENT/CANCEL/REFUND/SHIP/COMPLETE）
     * 2.执行订单后置处理（如缓存刷新、通知推送等）
     * 3.错误时进入重试逻辑
     * <p>
     * @author ZuiM
     * @param message 订单任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_ORDER)
    public void handleOrder(TaskMessage message) {
        try {
            log.info("📦 处理订单任务: {}", message.getTaskId());
            Map<String, Object> data = message.getData();
            Long orderId = ((Number) data.get("orderId")).longValue();
            String action = (String) data.get("action");

            // 根据操作类型执行后置处理
            switch (action) {
                case "PROCESS":
                    log.info("📦 订单 {} 创建完成，执行后置处理", orderId);
                    break;
                case "PAYMENT":
                    log.info("📦 订单 {} 支付成功，执行后置处理", orderId);
                    break;
                case "CANCEL":
                    log.info("📦 订单 {} 已取消，执行后置处理", orderId);
                    break;
                case "REFUND":
                    log.info("📦 订单 {} 已退款，执行后置处理", orderId);
                    break;
                case "SHIP":
                    log.info("📦 订单 {} 已发货，执行后置处理", orderId);
                    break;
                case "COMPLETE":
                    log.info("📦 订单 {} 已完成，执行后置处理", orderId);
                    break;
                default:
                    log.warn("📦 未知订单操作: {}", action);
            }
            log.info("✅ 订单处理成功: {}", orderId);
        } catch (Exception e) {
            log.error("❌ 订单处理失败: {}", e.getMessage());
            handleRetry(message);
        }
    }

    // ========== 4b. 消费缓存刷新任务 ==========
    /**
     * 消费缓存刷新任务
     * 1.解析缓存域（product/order/user/seller）和操作类型
     * 2.根据 key 列表执行 Redis 删除操作
     * 3.将缓存刷新从主业务流程中解耦
     * <p>
     * @author ZuiM
     * @param message 缓存刷新任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_CACHE)
    public void handleCacheRefresh(TaskMessage message) {
        try {
            log.info("🔄 处理缓存刷新任务: {}", message.getTaskId());
            Map<String, Object> data = message.getData();
            String domain = (String) data.get("domain");
            String action = (String) data.get("action");
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) data.get("keys");

            if (keys == null || keys.isEmpty()) {
                log.warn("🔄 缓存刷新任务无 key 列表，跳过: {}", message.getTaskId());
                return;
            }

            for (String key : keys) {
                if (key.endsWith("*")) {
                    // 模糊匹配删除
                    String prefix = key.substring(0, key.length() - 1);
                    redisUtil.delete(prefix);
                    log.debug("🔄 缓存模糊删除: {} from {}", prefix, domain);
                } else {
                    // 精确删除
                    redisUtil.delete(key);
                    log.debug("🔄 缓存精确删除: {} from {}", key, domain);
                }
            }
            log.info("✅ 缓存刷新成功: {} - {} ({} keys)", domain, action, keys.size());
        } catch (Exception e) {
            log.error("❌ 缓存刷新失败: {}", e.getMessage(), e);
        }
    }

    // ========== 5. 消费统计任务 ==========
    /**
     * 消费统计任务
     * 1.解析统计指标数据
     * 2.记录指标（TODO：落库/聚合）
     * <p>
     * @author ZuiM
     * @param message 统计任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_STATISTICS)
    public void handleStatistics(TaskMessage message) {
        try {
            Map<String, Object> data = message.getData();
            String metric = (String) data.get("metric");
            String dimension = (String) data.get("dimension");
            Number value = (Number) data.get("value");

            // TODO: 记录统计指标
            log.info("📊 统计指标: {} = {}", metric, value);
        } catch (Exception e) {
            log.error("❌ 统计记录失败: {}", e.getMessage());
        }
    }

    // ========== 6. 消费文件任务 ==========
    /**
     * 消费文件任务
     * 1.解析文件数据（fileId + operation）
     * 2.处理文件（TODO）
     * <p>
     * @author ZuiM
     * @param message 文件任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_FILE)
    public void handleFile(TaskMessage message) {
        try {
            Map<String, Object> data = message.getData();
            String fileId = (String) data.get("fileId");
            String operation = (String) data.get("operation");

            // TODO: 处理文件
            log.info("📁 文件处理: {} - {}", fileId, operation);
        } catch (Exception e) {
            log.error("❌ 文件处理失败: {}", e.getMessage());
        }
    }

    // ========== 7. 消费通知任务 ==========
    /**
     * 消费通知任务
     * 1.解析通知数据（userId + title + content）
     * 2.发送推送通知（TODO）
     * <p>
     * @author ZuiM
     * @param message 通知任务消息
     */
    @JmsListener(destination = ActiveMQConfig.QUEUE_NOTIFICATION)
    public void handleNotification(TaskMessage message) {
        try {
            Map<String, Object> data = message.getData();
            Long userId = ((Number) data.get("userId")).longValue();
            String title = (String) data.get("title");
            String content = (String) data.get("content");

            // TODO: 发送推送通知
            log.info("🔔 推送通知: {} - {}", userId, title);
        } catch (Exception e) {
            log.error("❌ 推送通知失败: {}", e.getMessage());
        }
    }

    // ========== 8. 消费广播消息 ==========
    /**
     * 消费广播消息（主题模式）
     * <p>
     * @author ZuiM
     * @param message 广播消息
     */
    @JmsListener(destination = ActiveMQConfig.TOPIC_BROADCAST, containerFactory = "topicListenerContainerFactory")
    public void handleBroadcast(TaskMessage message) {
        try {
            log.info("📢 收到广播消息: {}", message.getTaskType());
            // 处理广播逻辑
        } catch (Exception e) {
            log.error("❌ 广播消息处理失败: {}", e.getMessage());
        }
    }

    // ========== 9. 消费告警消息 ==========
    /**
     * 消费告警消息（主题模式）
     * 1.解析告警级别与内容
     * 2.发送钉钉/企微通知（TODO）
     * <p>
     * @author ZuiM
     * @param message 告警消息
     */
    @JmsListener(destination = ActiveMQConfig.TOPIC_ALERT, containerFactory = "topicListenerContainerFactory")
    public void handleAlert(TaskMessage message) {
        try {
            Map<String, Object> data = message.getData();
            String level = (String) data.get("level");
            String alertMessage = (String) data.get("message");
            log.info("🚨 收到告警: [{}] {}", level, alertMessage);
            // 发送钉钉/企微通知
        } catch (Exception e) {
            log.error("❌ 告警处理失败: {}", e.getMessage());
        }
    }

    // ========== 重试逻辑 ==========
    /**
     * 消息重试逻辑（最多 3 次，生产环境建议死信队列）
     * 1.重试次数 < 3：累加并重新发送（TODO）
     * 2.超过最大次数：记录失败日志，人工处理
     * <p>
     * @author ZuiM
     * @param message 待重试的消息
     */
    private void handleRetry(TaskMessage message) {
        int retryCount = message.getRetryCount();
        if (retryCount < 3) {
            message.setRetryCount(retryCount + 1);
            // 根据类型重新发送
            // 实际生产环境建议使用死信队列
            log.warn("🔄 消息重试: {} - 第{}次", message.getTaskId(), retryCount + 1);
        } else {
            log.error("❌ 消息处理失败，已超过最大重试次数: {}", message.getTaskId());
            // 记录失败日志，人工处理
        }
    }
}
