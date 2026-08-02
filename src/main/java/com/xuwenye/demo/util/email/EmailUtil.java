package com.xuwenye.demo.util.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    /** 邮箱格式正则（简单校验） */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 校验邮箱格式是否合法
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * 异步发送验证码邮件
     */
    @Async
    public void sendRegisterVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【XuWenYeTech】邮箱验证码");
            message.setText(
                    "您好！\n\n" +
                            "您正在进行账号注册，验证码为：\n\n" +
                            "    " + code + "\n\n" +
                            "该验证码 5 分钟内有效，请勿泄露给他人。\n\n" +
                            "如果不是您本人操作，请忽略此邮件。\n\n" +
                            "—— XuWenYeTech 团队"
            );
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 异步发送注销账号邮件
     */
    @Async
    public void sendDeleteAccountCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【XuWenYeTech】账号注销验证码");
            message.setText(
                    "您好！\n\n" +
                            "您正在进行账号注销，验证码为：\n\n" +
                            "    " + code + "\n\n" +
                            "该验证码 5 分钟内有效，请勿泄露给他人。\n\n" +
                            "如果不是您本人操作，请忽略此邮件。\n\n" +
                            "—— XuWenYeTech 团队"
            );
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 异步发送恢复账号邮件
     */
    @Async
    public void sendRecoverAccountCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【XuWenYeTech】账号恢复验证码");
            message.setText(
                    "您好！\n\n" +
                            "您正在进行账号恢复，验证码为：\n\n" +
                            "    " + code + "\n\n" +
                            "该验证码 5 分钟内有效，请勿泄露给他人。\n\n" +
                            "如果不是您本人操作，请忽略此邮件。\n\n" +
                            "—— XuWenYeTech 团队"
            );
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }
}
