package com.example.customerservice.config;

import com.alibaba.cloud.ai.tongyi.chat.TongYiChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类
 * 配置Spring Alibaba AI相关的Bean
 */
@Configuration
public class AiConfig {

    @Autowired
    private TongYiChatModel tongYiChatModel;

    /**
     * 配置ChatModel Bean
     * 使用通义千问作为底层AI模型
     */
    @Bean
    public ChatModel chatModel() {
        return tongYiChatModel;
    }
}
