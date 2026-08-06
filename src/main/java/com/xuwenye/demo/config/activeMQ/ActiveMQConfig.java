package com.xuwenye.demo.config.activeMQ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.Queue;
import jakarta.jms.Topic;

@Configuration
@EnableJms  // 启用 JMS 支持
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")
    private String password;

    // ========== 队列定义 ==========
    public static final String QUEUE_EMAIL = "queue.email";
    public static final String QUEUE_LOG = "queue.log";
    public static final String QUEUE_ORDER = "queue.order";
    public static final String QUEUE_SMS = "queue.sms";
    public static final String QUEUE_STATISTICS = "queue.statistics";
    public static final String QUEUE_FILE = "queue.file";
    public static final String QUEUE_NOTIFICATION = "queue.notification";

    // ========== 主题定义（发布订阅） ==========
    public static final String TOPIC_BROADCAST = "topic.broadcast";
    public static final String TOPIC_ALERT = "topic.alert";

    // ========== 1. 连接工厂 ==========
    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        // 设置信任所有包（允许序列化对象）
        factory.setTrustAllPackages(true);
        return factory;
    }

    // ========== 2. JMS 模板（发送消息） ==========
    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        // 默认使用队列模式
        template.setPubSubDomain(false);
        // 消息转换器
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }

    // ========== 3. JMS 消息发送模板（更高级） ==========
    @Bean
    public JmsMessagingTemplate jmsMessagingTemplate() {
        JmsMessagingTemplate template = new JmsMessagingTemplate();
        template.setJmsTemplate(jmsTemplate());
        return template;
    }

    // ========== 4. 消息监听容器工厂（队列） ==========
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("5-20");
        factory.setSessionTransacted(true);
        factory.setMessageConverter(jacksonMessageConverter());
        return factory;
    }

    // ========== 5. 消息监听容器工厂（主题） ==========
    @Bean
    public JmsListenerContainerFactory<?> topicListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("5-20");
        factory.setSessionTransacted(true);
        factory.setPubSubDomain(true);  // 开启主题模式
        factory.setMessageConverter(jacksonMessageConverter());
        return factory;
    }

    // ========== 6. 消息转换器（JSON） ==========
    @Bean
    public MessageConverter jacksonMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        ObjectMapper mapper = new ObjectMapper();
        // 注册Java8时间模块，处理LocalDateTime/LocalDate
        mapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(mapper);
        return converter;
    }

    // ========== 7. 队列 Bean ==========
    @Bean
    public Queue emailQueue() {
        return new ActiveMQQueue(QUEUE_EMAIL);
    }

    @Bean
    public Queue logQueue() {
        return new ActiveMQQueue(QUEUE_LOG);
    }

    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue(QUEUE_ORDER);
    }

    @Bean
    public Queue smsQueue() {
        return new ActiveMQQueue(QUEUE_SMS);
    }

    @Bean
    public Queue statisticsQueue() {
        return new ActiveMQQueue(QUEUE_STATISTICS);
    }

    @Bean
    public Queue fileQueue() {
        return new ActiveMQQueue(QUEUE_FILE);
    }

    @Bean
    public Queue notificationQueue() {
        return new ActiveMQQueue(QUEUE_NOTIFICATION);
    }

    // ========== 8. 主题 Bean ==========
    @Bean
    public Topic broadcastTopic() {
        return new ActiveMQTopic(TOPIC_BROADCAST);
    }

    @Bean
    public Topic alertTopic() {
        return new ActiveMQTopic(TOPIC_ALERT);
    }
}
