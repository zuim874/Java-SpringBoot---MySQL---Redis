package com.xuwenye.demo.Service;

import com.xuwenye.demo.config.activeMQ.ActiveMQConfig;
import com.xuwenye.demo.Entity.TaskMessage;
import com.xuwenye.demo.util.email.EmailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MQConsumer {
    private final EmailUtil emailUtil;
    public MQConsumer(EmailUtil emailUtil) {
        this.emailUtil = emailUtil;
    }

    // ========== 1. 消费邮件任务 ==========
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
//    @JmsListener(destination = ActiveMQConfig.QUEUE_LOG)
//    public void handleLog(TaskMessage message) {
//        try {
//            log.info("📝 处理日志任务: {}", message.getTaskId());
//            Map<String, Object> data = message.getData();
//            String username = (String) data.get("username");
//            String action = (String) data.get("action");
//            String detail = (String) data.get("detail");
//            String ip = (String) data.get("ip");
//
//            logService.saveOperationLog(username, action, detail, ip);
//            log.info("✅ 日志保存成功: {} -> {}", username, action);
//        } catch (Exception e) {
//            log.error("❌ 日志保存失败: {}", e.getMessage());
//        }
//    }

    // ========== 4. 消费订单任务 ==========
//    @JmsListener(destination = ActiveMQConfig.QUEUE_ORDER)
//    public void handleOrder(TaskMessage message) {
//        try {
//            log.info("📦 处理订单任务: {}", message.getTaskId());
//            Map<String, Object> data = message.getData();
//            Long orderId = ((Number) data.get("orderId")).longValue();
//            String action = (String) data.get("action");
//
//            if ("PROCESS".equals(action)) {
//                orderService.processOrder(orderId);
//            } else if ("PAYMENT".equals(action)) {
//                String tradeNo = (String) data.get("tradeNo");
//                String status = (String) data.get("status");
//                orderService.handlePayment(tradeNo, status);
//            }
//            log.info("✅ 订单处理成功: {}", orderId);
//        } catch (Exception e) {
//            log.error("❌ 订单处理失败: {}", e.getMessage());
//            handleRetry(message);
//        }
//    }

    // ========== 5. 消费统计任务 ==========
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
