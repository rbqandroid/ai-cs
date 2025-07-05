package com.example.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Alibaba AI 智能客服系统主应用程序
 * 
 * @author AI Assistant
 * @version 1.0.0
 */
@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("智能客服系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("=================================");
    }
}
