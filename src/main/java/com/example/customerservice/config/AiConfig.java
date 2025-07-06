package com.example.customerservice.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI配置类
 * 配置Spring Alibaba AI相关的Bean
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.dashscope.api-key:your-api-key-here}")
    private String apiKey;

    @Autowired(required = false)
    private DashScopeChatModel dashScopeChatModel;

    /**
     * 配置ChatModel Bean
     * 根据API密钥配置决定使用真实AI还是模拟AI
     */
    @Bean
    @Primary
    public ChatModel chatModel() {
        // 检查API密钥是否配置
        if (apiKey != null && !apiKey.equals("your-api-key-here") && !apiKey.trim().isEmpty()) {
            // 如果API密钥配置正确且DashScope模型可用，使用真实AI
            if (dashScopeChatModel != null) {
                return dashScopeChatModel;
            }
        }
        // 否则使用模拟AI
        return new MockChatModel();
    }
}
