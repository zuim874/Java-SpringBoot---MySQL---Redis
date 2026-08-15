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

/**
 * ActiveMQ 消息队列配置类
 * 1.定义连接工厂（broker-url/账号密码/信任所有包）
 * 2.定义 JMS 模板（发送消息）与消息监听容器工厂（消费消息）
 * 3.定义 JSON 消息转换器（支持 LocalDateTime）
 * 4.注册业务队列 Bean（邮件/日志/订单/短信/统计/文件/通知/缓存/优惠券）与主题 Bean（广播/告警）
 * <p>
 * @author ZuiM
 */
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
    public static final String QUEUE_CACHE = "queue.cache";
    public static final String QUEUE_COUPON = "queue.coupon";

    // ========== 主题定义（发布订阅） ==========
    public static final String TOPIC_BROADCAST = "topic.broadcast";
    public static final String TOPIC_ALERT = "topic.alert";

    // ========== 1. 连接工厂 ==========
    /**
     * ActiveMQ 连接工厂
     * 1.从配置读取 broker 地址、账号、密码
     * 2.设置信任所有包（允许序列化对象）
     * <p>
     * @author ZuiM
     * @return ActiveMQConnectionFactory 连接工厂
     */
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
    /**
     * JMS 模板（发送消息，队列模式）
     * 1.绑定连接工厂
     * 2.默认使用队列模式（pubSubDomain=false）
     * 3.设置 JSON 消息转换器
     * <p>
     * @author ZuiM
     * @return JmsTemplate 消息发送模板
     */
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
    /**
     * JMS 消息发送模板（高级封装，支持 convertAndSend）
     * <p>
     * @author ZuiM
     * @return JmsMessagingTemplate 高级消息发送模板
     */
    @Bean
    public JmsMessagingTemplate jmsMessagingTemplate() {
        JmsMessagingTemplate template = new JmsMessagingTemplate();
        template.setJmsTemplate(jmsTemplate());
        return template;
    }

    // ========== 4. 消息监听容器工厂（队列） ==========
    /**
     * 消息监听容器工厂（队列模式）
     * 1.绑定连接工厂
     * 2.设置并发消费者 5-20
     * 3.开启事务会话，设置 JSON 消息转换器
     * <p>
     * @author ZuiM
     * @return JmsListenerContainerFactory 队列监听容器工厂
     */
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
    /**
     * 消息监听容器工厂（主题/发布订阅模式）
     * 1.绑定连接工厂
     * 2.设置并发消费者 5-20
     * 3.开启主题模式（pubSubDomain=true）与事务会话
     * <p>
     * @author ZuiM
     * @return JmsListenerContainerFactory 主题监听容器工厂
     */
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
    /**
     * JSON 消息转换器
     * 1.目标类型为 TEXT
     * 2.注册 Java8 时间模块（支持 LocalDateTime/LocalDate）
     * <p>
     * @author ZuiM
     * @return MessageConverter JSON 消息转换器
     */
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
    /**
     * 邮件队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 邮件队列
     */
    @Bean
    public Queue emailQueue() {
        return new ActiveMQQueue(QUEUE_EMAIL);
    }

    /**
     * 日志队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 日志队列
     */
    @Bean
    public Queue logQueue() {
        return new ActiveMQQueue(QUEUE_LOG);
    }

    /**
     * 订单队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 订单队列
     */
    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue(QUEUE_ORDER);
    }

    /**
     * 短信队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 短信队列
     */
    @Bean
    public Queue smsQueue() {
        return new ActiveMQQueue(QUEUE_SMS);
    }

    /**
     * 统计队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 统计队列
     */
    @Bean
    public Queue statisticsQueue() {
        return new ActiveMQQueue(QUEUE_STATISTICS);
    }

    /**
     * 文件队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 文件队列
     */
    @Bean
    public Queue fileQueue() {
        return new ActiveMQQueue(QUEUE_FILE);
    }

    /**
     * 通知队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return new ActiveMQQueue(QUEUE_NOTIFICATION);
    }

    /**
     * 缓存刷新队列 Bean
     * <p>
     * @author ZuiM
     * @return Queue 缓存刷新队列
     */
    @Bean
    public Queue cacheQueue() {
        return new ActiveMQQueue(QUEUE_CACHE);
    }

    /**
     * 优惠券队列 Bean
     * 用于消费批量发放等费时任务（如向全部用户/VIP会员批量发券），异步解耦
     * <p>
     * @author ZuiM
     * @return Queue 优惠券队列
     */
    @Bean
    public Queue couponQueue() {
        return new ActiveMQQueue(QUEUE_COUPON);
    }

    // ========== 8. 主题 Bean ==========
    /**
     * 广播主题 Bean（所有订阅者都能收到）
     * <p>
     * @author ZuiM
     * @return Topic 广播主题
     */
    @Bean
    public Topic broadcastTopic() {
        return new ActiveMQTopic(TOPIC_BROADCAST);
    }

    /**
     * 告警主题 Bean（所有订阅者都能收到）
     * <p>
     * @author ZuiM
     * @return Topic 告警主题
     */
    @Bean
    public Topic alertTopic() {
        return new ActiveMQTopic(TOPIC_ALERT);
    }
}
