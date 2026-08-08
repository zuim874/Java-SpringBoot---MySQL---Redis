package com.xuwenye.demo.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密码强度检测配置（读取 test.passwordstrength 配置前缀）
 * <p>
 * @author ZuiM
 */
@Component
@ConfigurationProperties(prefix = "test.passwordstrength")
public class PasswordStrengthConfig {
    private boolean status = false; // 密码强度检测默认为关闭

    /**
     * 获取密码强度检测开关
     * <p>
     * @author ZuiM
     * @return boolean true=开启检测
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * 设置密码强度检测开关
     * <p>
     * @author ZuiM
     * @param status true=开启检测
     */
    public void setStatus(boolean status) {
        this.status = status;
    }
}
