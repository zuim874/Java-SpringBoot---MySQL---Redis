package com.xuwenye.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class EmailTest {
    @Value("${spring.mail.username}")
    private String mailUserName;

    @Value("${test.mail.username}")
    private String testMailUserName;

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testSendMail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUserName);
        message.setTo(testMailUserName);
        message.setSubject("测试邮件");
        message.setText("这是一封测试邮件，如果收到说明配置成功！");
        mailSender.send(message);
        System.out.println("邮件发送成功！");
    }
}