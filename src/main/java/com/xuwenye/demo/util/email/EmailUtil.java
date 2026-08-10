package com.xuwenye.demo.util.email;

import com.xuwenye.demo.util.codeGenerator.GenerateVerificationCode;
import com.xuwenye.demo.util.oi.SanitizeUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具
 * 1.isValidEmail：静态校验邮箱格式
 * 2.sendVerificationCode：异步发送验证码邮件（生成验证码 + 存 Redis + 发邮件）
 * <p>
 * @author ZuiM
 */
@Component
public class EmailUtil {

    /** 邮箱格式正则（简单校验） */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final JavaMailSender mailSender;
    private final GenerateVerificationCode generateVerificationCode;
    private final RedisUtil redisUtil;
    private final SanitizeUtil sanitizeUtil;

    public EmailUtil(JavaMailSender mailSender,
                     GenerateVerificationCode generateVerificationCode,
                     RedisUtil redisUtil, SanitizeUtil sanitizeUtil) {
        this.mailSender = mailSender;
        this.generateVerificationCode = generateVerificationCode;
        this.redisUtil = redisUtil;
        this.sanitizeUtil = sanitizeUtil;
    }

    // 发信人邮箱
    @Value("${spring.mail.username}")
    private String fromEmail;
    // 过期时长
    @Value("${test.redisTimeOut}")
    private String redisTimeOut;

    /**
     * 校验邮箱格式是否合法
     * <p>
     * @author ZuiM
     * @param email 待检测格式是否合法的邮箱
     * @return boolean true=格式合法
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * 异步发送验证码邮件
     * 1.根据 emailTypeCode 获取邮件类型枚举
     * 2.需要验证码时：生成 6 位验证码并存入 Redis（key 规则 verify_{template}Code:{邮箱}）
     * 3.拼接邮件正文并发送
     * <p>
     * @author ZuiM
     * @param toEmail 收信人邮箱
     * @param emailTypeCode 邮件类型编码（见 EmailType）
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
                String template = type.getTemplate();
                String code = generateVerificationCode.generateVerificationCode();  // 生成六位数验证码
                String realEmail = sanitizeUtil.dealEmail(toEmail);
                String redisKey = "verify_" + template + "Code:" + realEmail;
                redisUtil.setCode(redisKey, code);

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
