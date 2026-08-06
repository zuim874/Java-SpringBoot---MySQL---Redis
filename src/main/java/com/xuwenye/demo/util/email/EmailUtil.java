package com.xuwenye.demo.util.email;

import com.xuwenye.demo.util.codeGenerator.GenerateVerificationCode;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    /** 邮箱格式正则（简单校验） */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final JavaMailSender mailSender;
    private final GenerateVerificationCode generateVerificationCode;
    private final RedisUtil redisUtil;

    public EmailUtil(JavaMailSender mailSender,
                     GenerateVerificationCode generateVerificationCode,
                     RedisUtil redisUtil) {
        this.mailSender = mailSender;
        this.generateVerificationCode = generateVerificationCode;
        this.redisUtil = redisUtil;
    }

    // 发信人邮箱
    @Value("${spring.mail.username}")
    private String fromEmail;
    // 过期时长
    @Value("${test.redisTimeOut}")
    private String redisTimeOut;

    /**
     * @param email 待检测格式是否合法的邮箱
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * @param toEmail 收信人邮箱
     * @param emailTypeCode 邮件类型编码
     */
    @Async
    public void sendVerificationCode(String toEmail, Integer emailTypeCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            EmailType type = EmailType.getByCode(emailTypeCode);    // 邮箱实例
            String title = type.getTitle();   // 获取邮件标题
            String description = type.getDescription(); // 获取邮件描述
            message.setSubject("【XuWenYeTech】" + title + "验证码");

            Boolean needCode = type.getNeedVerify();    // 邮箱是否需要验证码
            if (needCode) {
                String code = generateVerificationCode.generateVerificationCode();  // 生成六位数验证码
                String redisKey = "verify_" + "Code:" + toEmail;
                redisUtil.set(redisKey, code);

                message.setText(
                        "您好！\n\n" +
                                "您正在进行 " + description + " ，验证码为：\n\n" +
                                "    " + code + "\n\n" +
                                "该验证码 " + redisTimeOut + " 分钟内有效，请勿泄露给他人。\n\n" +
                                "如果不是您本人操作，请忽略此邮件。\n\n" +
                                "—— XuWenYeTech 团队"
                );
            }
            else {
                message.setText(
                        "您好！\n\n" +
                                description + " \n\n" +
                                "—— XuWenYeTech 团队"
                );
            }

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }
}
