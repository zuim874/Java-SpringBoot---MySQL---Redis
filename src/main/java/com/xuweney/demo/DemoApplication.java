package com.xuweney.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan  // 扫描 @WebFilter、@WebServlet、@WebListener
public class DemoApplication {
    public static void main(String[] args){
        SpringApplication.run(DemoApplication.class, args);
    }
}