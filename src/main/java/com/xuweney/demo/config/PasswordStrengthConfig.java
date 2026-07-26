package com.xuweney.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "test.passwordstrength")
public class PasswordStrengthConfig {
    private boolean status = false; // 密码强度检测默认为关闭

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
