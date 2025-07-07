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
 * AI配置类 - Spring Alibaba AI集成配置
 *
 * <p>这个配置类负责设置和配置Spring Alibaba AI框架中的AI模型组件。
 * 主要功能是将阿里云DashScope（通义千问）模型集成到Spring应用程序中，
 * 并提供智能的降级机制：当API密钥未配置或无效时，自动切换到模拟AI模式。</p>
 *
 * <h3>配置策略：</h3>
 * <ul>
 *   <li>优先使用真实的DashScope AI模型</li>
 *   <li>API密钥无效时自动降级到模拟AI</li>
 *   <li>支持开发和生产环境的无缝切换</li>
 * </ul>
 *
 * <h3>依赖要求：</h3>
 * <ul>
 *   <li>spring-ai-alibaba-starter</li>
 *   <li>有效的DashScope API密钥（可选）</li>
 *   <li>网络连接到阿里云服务（使用真实AI时）</li>
 * </ul>
 *
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-04
 * @see DashScopeChatModel
 * @see ChatModel
 */
@Configuration
public class AiConfig {

    /**
     * DashScope API密钥
     *
     * <p>从配置文件中读取API密钥，支持环境变量和配置文件两种方式。
     * 如果未配置或配置为默认值，系统将自动使用模拟AI模式。</p>
     *
     * <h3>配置方式：</h3>
     * <ul>
     *   <li>application.yml: spring.ai.dashscope.api-key</li>
     *   <li>环境变量: DASHSCOPE_API_KEY</li>
     *   <li>命令行参数: --spring.ai.dashscope.api-key=your-key</li>
     * </ul>
     */
    @Value("${spring.ai.dashscope.api-key:your-api-key-here}")
    private String apiKey;

    /**
     * DashScope聊天模型实例
     *
     * <p>这个实例由Spring Alibaba AI框架自动创建和配置。
     * 使用@Autowired(required = false)确保即使DashScope不可用时，
     * 应用程序也能正常启动。</p>
     */
    @Autowired(required = false)
    private DashScopeChatModel dashScopeChatModel;

    /**
     * 配置ChatModel Bean - 智能AI模型选择
     *
     * <p>这个方法实现了智能的AI模型选择策略：</p>
     * <ol>
     *   <li>首先检查API密钥是否有效配置</li>
     *   <li>如果API密钥有效且DashScope模型可用，使用真实AI</li>
     *   <li>否则自动降级到模拟AI模式，确保系统可用性</li>
     * </ol>
     *
     * <h3>真实AI模式特性：</h3>
     * <ul>
     *   <li>基于阿里云通义千问大语言模型</li>
     *   <li>支持多轮对话和上下文理解</li>
     *   <li>中文优化，响应质量高</li>
     *   <li>实时API调用，消耗Token</li>
     * </ul>
     *
     * <h3>模拟AI模式特性：</h3>
     * <ul>
     *   <li>基于关键词匹配的规则回复</li>
     *   <li>无需API密钥，完全离线</li>
     *   <li>适合开发测试环境</li>
     *   <li>零成本运行</li>
     * </ul>
     *
     * @return ChatModel 配置好的聊天模型实例（真实AI或模拟AI）
     * @throws RuntimeException 如果模拟AI创建失败
     */
    @Bean
    @Primary
    public ChatModel chatModel() {
        // 检查API密钥是否配置
        if (apiKey != null && !apiKey.equals("your-api-key-here") && !apiKey.trim().isEmpty()) {
            // 如果API密钥配置正确且DashScope模型可用，使用真实AI
            if (dashScopeChatModel != null) {
                System.out.println("✅ 使用真实DashScope AI模型");
                return dashScopeChatModel;
            }
        }
        // 否则使用模拟AI
        System.out.println("⚠️ API密钥未配置或无效，使用模拟AI模式");
        return new MockChatModel();
    }
}
