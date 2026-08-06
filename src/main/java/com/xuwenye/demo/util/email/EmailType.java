package com.xuwenye.demo.util.email;

import lombok.Getter;

/**
 * 邮件类型枚举
 */
@Getter
public enum EmailType {
    /**
     * 枚举常量实例：必须放在枚举类的开头，写在构造方法，成员变量，所有方法前面
     */
    // 0.注册验证
    REGISTER(0, "注册验证", "注册验证", true, "register"),
    // 1.账号恢复验证
    RECOVER(1, "账号恢复", "账号恢复" ,true, "recover"),
    // 2.账号注销验证
    DELETE(2, "账号注销", "账号注销", true, "delete"),

    // 999.测试
    TEST(999, "测试", "这是一封测试邮件", false, "test");


    // 状态码，描述，是否需要验证码，模板名称
    private final Integer code;
    private final String title;
    private final String description;
    private final Boolean needVerify;
    private final String template;

    EmailType(Integer code,String title, String description, Boolean needVerify, String template) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.needVerify = needVerify;
        this.template = template;
    }

    // 根据code得到EmailType
    public static EmailType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (EmailType type: EmailType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    // 根据description得到EmailType
    public static EmailType getByDescription(String description) {
        if (description == null) {
            return null;
        }
        for (EmailType type: EmailType.values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}