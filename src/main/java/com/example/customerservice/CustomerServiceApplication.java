package com.example.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Alibaba AI 智能客服系统主应用程序
 *
 * <p>这是一个基于Spring Boot 3.3.3和Spring Alibaba AI框架构建的智能客服系统。
 * 系统集成了阿里云通义千问大语言模型，提供智能化的客服对话功能。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>智能对话：基于通义千问的自然语言处理</li>
 *   <li>会话管理：支持多用户并发会话</li>
 *   <li>消息持久化：完整的对话历史记录</li>
 *   <li>RESTful API：标准化的HTTP接口</li>
 *   <li>Web界面：友好的用户交互界面</li>
 * </ul>
 *
 * <h3>技术栈：</h3>
 * <ul>
 *   <li>Spring Boot 3.3.3</li>
 *   <li>Spring Alibaba AI</li>
 *   <li>JDK 21</li>
 *   <li>Maven 3.9.10</li>
 *   <li>H2 Database</li>
 *   <li>Thymeleaf</li>
 * </ul>
 *
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-04
 */
@SpringBootApplication
public class CustomerServiceApplication {

    /**
     * 应用程序入口点
     *
     * <p>启动Spring Boot应用程序，初始化所有必要的组件和服务。
     * 应用启动后会在控制台显示访问信息。</p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用程序
        SpringApplication.run(CustomerServiceApplication.class, args);

        // 显示启动成功信息
        System.out.println("=================================");
        System.out.println("智能客服系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("API文档: http://localhost:8080/api/chat/health");
        System.out.println("数据库控制台: http://localhost:8080/h2-console");
        System.out.println("=================================");
    }
}
