package com.xuwenye.demo.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuwenye.demo.config.activeMQ.ActiveMQConfig;
import com.xuwenye.demo.Entity.TaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.jms.Queue;
import jakarta.jms.Topic;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MQProducer {
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final Queue emailQueue;
    private final Queue logQueue;
    private final Queue orderQueue;
    private final Queue smsQueue;
    private final Queue statisticsQueue;
    private final Queue fileQueue;
    private final Queue notificationQueue;
    private final Topic broadcastTopic;
    private final Topic alertTopic;
    public MQProducer(JmsMessagingTemplate jmsMessagingTemplate,
                      Queue emailQueue,
                      Queue logQueue,
                      Queue orderQueue,
                      Queue smsQueue,
                      Queue statisticsQueue,
                      Queue fileQueue,
                      Queue notificationQueue,
                      Topic broadcastTopic,
                      Topic alertTopic) {
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.emailQueue = emailQueue;
        this.logQueue = logQueue;
        this.orderQueue = orderQueue;
        this.smsQueue = smsQueue;
        this.statisticsQueue = statisticsQueue;
        this.fileQueue = fileQueue;
        this.notificationQueue = notificationQueue;
        this.broadcastTopic = broadcastTopic;
        this.alertTopic = alertTopic;
    }

    // ========== 1. 发送邮件任务 ==========
    @Async
    public void sendEmailTask(String toEmail, Integer emailTypeCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("toEmail", toEmail);
        data.put("emailTypeCode", emailTypeCode);

        sendMessage(emailQueue, "EMAIL", "VERIFY", data);
        log.info("📧 邮件任务已发送: {}", toEmail);
    }

    // ========== 2. 发送短信任务 ==========
    @Async
    public void sendSmsTask(String phone, String code) {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("code", code);
        data.put("type", "VERIFY_CODE");

        sendMessage(smsQueue, "SMS", "VERIFY", data);
        log.info("📱 短信任务已发送: {}", phone);
    }

    // ========== 3. 发送日志任务 ==========
    @Async
    public void sendLogTask(String username, String action, String detail) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("action", action);
        data.put("detail", detail);
        data.put("ip", getClientIp());
        data.put("timestamp", System.currentTimeMillis());

        sendMessage(logQueue, "LOG", action, data);
        log.info("📝 日志任务已发送: {} -> {}", username, action);
    }

    // ========== 4. 发送订单任务 ==========
    @Async
    public void sendOrderTask(Long orderId, String action) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("action", action);

        sendMessage(orderQueue, "ORDER", action, data);
        log.info("📦 订单任务已发送: {} - {}", orderId, action);
    }

    // ========== 5. 发送统计任务 ==========
    @Async
    public void sendStatisticsTask(String metric, String dimension, Number value) {
        Map<String, Object> data = new HashMap<>();
        data.put("metric", metric);
        data.put("dimension", dimension);
        data.put("value", value);
        data.put("timestamp", System.currentTimeMillis());

        sendMessage(statisticsQueue, "STATISTICS", metric, data);
    }

    // ========== 6. 发送文件任务 ==========
    @Async
    public void sendFileTask(String fileId, String operation) {
        Map<String, Object> data = new HashMap<>();
        data.put("fileId", fileId);
        data.put("operation", operation);

        sendMessage(fileQueue, "FILE", operation, data);
        log.info("📁 文件任务已发送: {} - {}", fileId, operation);
    }

    // ========== 7. 发送通知任务 ==========
    @Async
    public void sendNotificationTask(Long userId, String title, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", title);
        data.put("content", content);
        data.put("type", "PUSH");

        sendMessage(notificationQueue, "NOTIFICATION", "PUSH", data);
        log.info("🔔 通知任务已发送: {} - {}", userId, title);
    }

    // ========== 8. 发送广播消息（所有订阅者收到） ==========
    @Async
    public void sendBroadcast(String event, Map<String, Object> data) {
        sendMessageToTopic(broadcastTopic, "BROADCAST", event, data);
        log.info("📢 广播消息已发送: {}", event);
    }

    // ========== 9. 发送告警消息 ==========
    @Async
    public void sendAlert(String level, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());

        sendMessageToTopic(alertTopic, "ALERT", level, data);
        log.info("🚨 告警消息已发送: {} - {}", level, message);
    }

    // ========== 通用发送方法（队列） ==========
    private void sendMessage(Queue queue, String taskType, String businessType, Map<String, Object> data) {
        TaskMessage message = new TaskMessage();
        message.setTaskId(UUID.randomUUID().toString());
        message.setTaskType(taskType);
        message.setBusinessType(businessType);
        message.setData(data);
        message.setCreateTime(LocalDateTime.now());
        message.setOperator(getCurrentUser());

        jmsMessagingTemplate.convertAndSend(queue, message);
    }

    // ========== 通用发送方法（主题） ==========
    private void sendMessageToTopic(Topic topic, String taskType, String businessType, Map<String, Object> data) {
        TaskMessage message = new TaskMessage();
        message.setTaskId(UUID.randomUUID().toString());
        message.setTaskType(taskType);
        message.setBusinessType(businessType);
        message.setData(data);
        message.setCreateTime(LocalDateTime.now());

        jmsMessagingTemplate.convertAndSend(topic, message);
    }

    // ========== 工具方法 ==========
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }

    private String getCurrentUser() {
        // TODO: 从 SecurityContext 获取当前用户
        return "system";
    }
}
