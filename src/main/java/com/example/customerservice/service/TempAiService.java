package com.example.customerservice.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 临时AI服务 - 模拟Spring AI接口
 * 当Spring AI Alibaba依赖可用时，将被真实实现替换
 */
@Service
public class TempAiService {
    
    /**
     * 模拟ChatModel的call方法
     */
    public ChatResponse call(Prompt prompt) {
        // 获取用户消息
        List<Message> messages = prompt.getInstructions();
        String userMessage = "";
        
        for (Message message : messages) {
            if (message.getContent() != null) {
                userMessage = message.getContent();
                break;
            }
        }
        
        // 生成模拟回复
        String reply = generateMockResponse(userMessage.toLowerCase());
        
        // 创建模拟的ChatResponse
        return new MockChatResponse(reply);
    }
    
    private String generateMockResponse(String userMessage) {
        if (userMessage.contains("你好") || userMessage.contains("hello")) {
            return "您好！我是智能客服助手，很高兴为您服务。请问有什么可以帮助您的吗？";
        } else if (userMessage.contains("产品") || userMessage.contains("服务")) {
            return "我们提供多种优质的产品和服务。请告诉我您具体想了解哪方面的信息，我会为您详细介绍。";
        } else if (userMessage.contains("价格") || userMessage.contains("费用") || userMessage.contains("多少钱")) {
            return "关于价格信息，我建议您联系我们的销售团队获取最新的报价。您可以提供具体需求，我们会为您制定合适的方案。";
        } else if (userMessage.contains("联系") || userMessage.contains("电话") || userMessage.contains("客服")) {
            return "您可以通过以下方式联系我们：\n- 客服热线：400-123-4567\n- 邮箱：service@example.com\n- 工作时间：周一至周五 9:00-18:00";
        } else if (userMessage.contains("谢谢") || userMessage.contains("感谢")) {
            return "不客气！很高兴能够帮助您。如果您还有其他问题，随时可以咨询我。";
        } else if (userMessage.contains("再见") || userMessage.contains("拜拜")) {
            return "再见！感谢您的咨询，祝您生活愉快！如有需要，欢迎随时联系我们。";
        } else {
            return "感谢您的咨询！我已经收到您的问题：\"" + userMessage + "\"。我会尽力为您提供帮助。如需更详细的信息，建议您联系我们的专业客服团队。";
        }
    }
    
    /**
     * 模拟ChatResponse类
     */
    private static class MockChatResponse implements ChatResponse {
        private final String content;
        
        public MockChatResponse(String content) {
            this.content = content;
        }
        
        @Override
        public org.springframework.ai.chat.model.ChatResponse.Result getResult() {
            return new MockResult(content);
        }
        
        @Override
        public java.util.List<org.springframework.ai.chat.model.ChatResponse.Result> getResults() {
            return java.util.List.of(getResult());
        }
        
        @Override
        public org.springframework.ai.model.ResponseMetadata getMetadata() {
            return new MockMetadata();
        }
    }
    
    /**
     * 模拟Result类
     */
    private static class MockResult implements ChatResponse.Result {
        private final String content;
        
        public MockResult(String content) {
            this.content = content;
        }
        
        @Override
        public org.springframework.ai.chat.messages.AssistantMessage getOutput() {
            return new org.springframework.ai.chat.messages.AssistantMessage(content);
        }
        
        @Override
        public org.springframework.ai.model.ResponseMetadata getMetadata() {
            return new MockMetadata();
        }
    }
    
    /**
     * 模拟Metadata类
     */
    private static class MockMetadata implements org.springframework.ai.model.ResponseMetadata {
        @Override
        public org.springframework.ai.model.Usage getUsage() {
            return new MockUsage();
        }
    }
    
    /**
     * 模拟Usage类
     */
    private static class MockUsage implements org.springframework.ai.model.Usage {
        @Override
        public Long getPromptTokens() {
            return 50L;
        }
        
        @Override
        public Long getGenerationTokens() {
            return 100L;
        }
        
        @Override
        public Long getTotalTokens() {
            return 150L;
        }
    }
}
