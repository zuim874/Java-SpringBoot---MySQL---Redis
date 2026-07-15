package com.xuweney.demo.Controller;

import com.xuweney.demo.common.Result;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hello")
    public Result<String> hello(Authentication authentication) {
        String username = authentication.getName();
        return Result.ok("✅ 登录成功！当前用户：" + username);
    }
}