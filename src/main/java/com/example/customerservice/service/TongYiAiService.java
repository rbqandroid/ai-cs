package com.example.customerservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 通义千问AI服务
 * 直接调用通义千问API实现智能对话功能
 */
@Service
public class TongYiAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(TongYiAiService.class);
    
    @Value("${tongyi.api.key:your-api-key-here}")
    private String apiKey;
    
    @Value("${tongyi.api.url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;
    
    @Value("${tongyi.model:qwen-turbo}")
    private String model;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public TongYiAiService() {
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 调用通义千问进行对话
     */
    public String chat(String message) {
        try {
            // 如果API密钥未配置，返回模拟回复
            if (apiKey == null || apiKey.equals("your-api-key-here") || apiKey.trim().isEmpty()) {
                logger.warn("通义千问API密钥未配置，使用模拟回复");
                return generateMockResponse(message);
            }
            
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(message);
            
            // 调用API
            Mono<String> responseMono = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-SSE", "disable")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30));
            
            String response = responseMono.block();
            return parseResponse(response);
            
        } catch (Exception e) {
            logger.error("调用通义千问API失败", e);
            return generateErrorResponse(e.getMessage());
        }
    }
    
    /**
     * 构建API请求体
     */
    private Map<String, Object> buildRequestBody(String message) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", message);
        requestBody.put("input", input);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", 1000);
        parameters.put("temperature", 0.7);
        parameters.put("top_p", 0.9);
        requestBody.put("parameters", parameters);
        
        return requestBody;
    }
    
    /**
     * 解析API响应
     */
    private String parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查是否有错误
            if (jsonNode.has("code") && !jsonNode.get("code").asText().equals("200")) {
                String errorMsg = jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误";
                logger.error("通义千问API返回错误: {}", errorMsg);
                return "抱歉，AI服务暂时不可用，请稍后重试。";
            }
            
            // 提取回复内容
            JsonNode output = jsonNode.path("output");
            if (output.has("text")) {
                return output.get("text").asText();
            } else if (output.has("choices") && output.get("choices").isArray() && output.get("choices").size() > 0) {
                JsonNode firstChoice = output.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            
            logger.warn("无法解析通义千问API响应: {}", response);
            return "抱歉，我暂时无法理解您的问题，请重新表述。";
            
        } catch (Exception e) {
            logger.error("解析通义千问API响应失败", e);
            return "抱歉，处理您的请求时出现了问题。";
        }
    }
    
    /**
     * 生成模拟回复（当API不可用时）
     */
    private String generateMockResponse(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("你好") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "您好！我是智能客服助手，很高兴为您服务。请问有什么可以帮助您的吗？";
        } else if (lowerMessage.contains("产品") || lowerMessage.contains("服务") || lowerMessage.contains("功能")) {
            return "我们提供多种优质的产品和服务。请告诉我您具体想了解哪方面的信息，我会为您详细介绍。";
        } else if (lowerMessage.contains("价格") || lowerMessage.contains("费用") || lowerMessage.contains("多少钱") || lowerMessage.contains("收费")) {
            return "关于价格信息，我建议您联系我们的销售团队获取最新的报价。您可以提供具体需求，我们会为您制定合适的方案。";
        } else if (lowerMessage.contains("联系") || lowerMessage.contains("电话") || lowerMessage.contains("客服")) {
            return "您可以通过以下方式联系我们：\n- 客服热线：400-123-4567\n- 邮箱：service@example.com\n- 工作时间：周一至周五 9:00-18:00";
        } else if (lowerMessage.contains("问题") || lowerMessage.contains("故障") || lowerMessage.contains("不能") || lowerMessage.contains("错误")) {
            return "我理解您遇到了技术问题。请详细描述一下具体的问题现象，我会尽力为您提供解决方案。如果问题复杂，我也可以为您转接技术支持专员。";
        } else if (lowerMessage.contains("谢谢") || lowerMessage.contains("感谢") || lowerMessage.contains("thanks")) {
            return "不客气！很高兴能够帮助您。如果您还有其他问题，随时可以咨询我。";
        } else if (lowerMessage.contains("再见") || lowerMessage.contains("拜拜") || lowerMessage.contains("bye")) {
            return "再见！感谢您的咨询，祝您生活愉快！如有需要，欢迎随时联系我们。";
        } else {
            return String.format("感谢您的咨询！我已经收到您的问题：\"%s\"。我会尽力为您提供帮助。如需更详细的信息，建议您联系我们的专业客服团队。", 
                message.length() > 50 ? message.substring(0, 50) + "..." : message);
        }
    }
    
    /**
     * 生成错误回复
     */
    private String generateErrorResponse(String errorMessage) {
        return "抱歉，AI服务暂时不可用。请稍后重试，或联系人工客服获得帮助。";
    }
    
    /**
     * 检查API配置是否有效
     */
    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.equals("your-api-key-here") && !apiKey.trim().isEmpty();
    }
    
    /**
     * 获取服务状态
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("apiConfigured", isApiConfigured());
        status.put("model", model);
        status.put("apiUrl", apiUrl);
        status.put("status", isApiConfigured() ? "READY" : "MOCK_MODE");
        return status;
    }
}
