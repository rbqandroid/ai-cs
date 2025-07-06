package com.example.customerservice.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * 模拟ChatModel实现
 * 当DashScope API密钥未配置时使用
 */
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        // 获取用户消息
        String userMessage = "";
        if (!prompt.getInstructions().isEmpty()) {
            Message lastMessage = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
            if (lastMessage instanceof org.springframework.ai.chat.messages.UserMessage) {
                userMessage = ((org.springframework.ai.chat.messages.UserMessage) lastMessage).getText();
            } else {
                userMessage = lastMessage.toString();
            }
        }

        // 生成模拟回复
        String mockReply = generateMockResponse(userMessage);

        // 创建AssistantMessage
        AssistantMessage assistantMessage = new AssistantMessage(mockReply);

        // 创建Generation
        Generation generation = new Generation(assistantMessage);

        // 创建ChatResponse
        return new ChatResponse(List.of(generation));
    }

    private String generateMockResponse(String userMessage) {
        String message = userMessage.toLowerCase();

        if (message.contains("你好") || message.contains("hello") || message.contains("hi")) {
            return "您好！我是智能客服助手，很高兴为您服务。请问有什么可以帮助您的吗？";
        } else if (message.contains("产品") || message.contains("服务") || message.contains("功能")) {
            return "我们提供多种优质的产品和服务。请告诉我您具体想了解哪方面的信息，我会为您详细介绍。";
        } else if (message.contains("价格") || message.contains("费用") || message.contains("多少钱") || message.contains("收费")) {
            return "关于价格信息，我建议您联系我们的销售团队获取最新的报价。您可以提供具体需求，我们会为您制定合适的方案。";
        } else if (message.contains("联系") || message.contains("电话") || message.contains("客服")) {
            return "您可以通过以下方式联系我们：\n- 客服热线：400-123-4567\n- 邮箱：service@example.com\n- 工作时间：周一至周五 9:00-18:00";
        } else if (message.contains("问题") || message.contains("故障") || message.contains("不能") || message.contains("错误")) {
            return "我理解您遇到了技术问题。请详细描述一下具体的问题现象，我会尽力为您提供解决方案。如果问题复杂，我也可以为您转接技术支持专员。";
        } else if (message.contains("谢谢") || message.contains("感谢") || message.contains("thanks")) {
            return "不客气！很高兴能够帮助您。如果您还有其他问题，随时可以咨询我。";
        } else if (message.contains("再见") || message.contains("拜拜") || message.contains("bye")) {
            return "再见！感谢您的咨询，祝您生活愉快！如有需要，欢迎随时联系我们。";
        } else {
            return String.format("感谢您的咨询！我已经收到您的问题：\"%s\"。我会尽力为您提供帮助。如需更详细的信息，建议您联系我们的专业客服团队。", 
                message.length() > 50 ? message.substring(0, 50) + "..." : message);
        }
    }

    @Override
    public String call(String message) {
        Prompt prompt = new Prompt(message);
        ChatResponse response = call(prompt);
        return response.getResult().getOutput().getText();
    }
}
