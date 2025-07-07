package com.example.customerservice.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * 模拟聊天模型实现
 *
 * <p>这个类提供了一个完全离线的AI聊天模型实现，用于在没有真实API密钥时
 * 提供基本的聊天功能。它基于关键词匹配提供预定义的回复，
 * 确保系统在开发和测试环境中的可用性。</p>
 *
 * <h3>主要特性：</h3>
 * <ul>
 *   <li>完全离线运行，无需网络连接</li>
 *   <li>基于关键词匹配的智能回复</li>
 *   <li>支持中文和英文对话</li>
 *   <li>模拟真实AI的响应格式</li>
 *   <li>零成本运行</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>开发环境测试</li>
 *   <li>演示和原型验证</li>
 *   <li>API密钥未配置时的降级方案</li>
 *   <li>离线环境部署</li>
 * </ul>
 *
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-04
 * @see ChatModel
 * @see ChatResponse
 */
public class MockChatModel implements ChatModel {

    /**
     * 处理聊天请求并生成回复
     *
     * <p>这个方法分析用户输入的消息，基于关键词匹配生成相应的回复。
     * 支持多种常见的客服场景，包括问候、产品咨询、价格询问等。</p>
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>从Prompt中提取用户消息</li>
     *   <li>基于关键词匹配生成回复</li>
     *   <li>创建标准的ChatResponse对象</li>
     *   <li>返回格式化的响应</li>
     * </ol>
     *
     * @param prompt 包含用户消息的提示对象
     * @return ChatResponse 包含AI回复的响应对象
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        // 获取用户消息 - 从指令列表中提取最后一条用户消息
        String userMessage = "";
        if (!prompt.getInstructions().isEmpty()) {
            Message lastMessage = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
            if (lastMessage instanceof org.springframework.ai.chat.messages.UserMessage) {
                userMessage = ((org.springframework.ai.chat.messages.UserMessage) lastMessage).getText();
            } else {
                userMessage = lastMessage.toString();
            }
        }

        // 生成模拟回复 - 基于关键词匹配算法
        String mockReply = generateMockResponse(userMessage);

        // 创建AssistantMessage - 模拟AI助手的回复消息
        AssistantMessage assistantMessage = new AssistantMessage(mockReply);

        // 创建Generation - 包装助手消息为生成结果
        Generation generation = new Generation(assistantMessage);

        // 创建ChatResponse - 返回标准的聊天响应格式
        return new ChatResponse(List.of(generation));
    }

    /**
     * 基于关键词匹配生成模拟回复
     *
     * <p>这个方法包含了丰富的关键词匹配规则，能够对常见的客服场景
     * 提供合适的回复。支持中文和英文，涵盖问候、咨询、感谢等多种情况。</p>
     *
     * <h3>支持的场景类型：</h3>
     * <ul>
     *   <li>问候类：你好、hello、hi等</li>
     *   <li>产品咨询：产品、服务、功能等</li>
     *   <li>价格询问：价格、费用、多少钱等</li>
     *   <li>联系方式：联系、电话、客服等</li>
     *   <li>技术支持：问题、故障、错误等</li>
     *   <li>感谢表达：谢谢、感谢、thanks等</li>
     *   <li>告别用语：再见、拜拜、bye等</li>
     * </ul>
     *
     * @param userMessage 用户输入的原始消息
     * @return 生成的回复消息
     */
    private String generateMockResponse(String userMessage) {
        // 转换为小写以便进行大小写不敏感的匹配
        String message = userMessage.toLowerCase();

        // 问候类消息处理
        if (message.contains("你好") || message.contains("hello") || message.contains("hi")) {
            return "您好！我是智能客服助手，很高兴为您服务。请问有什么可以帮助您的吗？";
        }
        // 产品和服务咨询处理
        else if (message.contains("产品") || message.contains("服务") || message.contains("功能")) {
            return "我们提供多种优质的产品和服务。请告诉我您具体想了解哪方面的信息，我会为您详细介绍。";
        }
        // 价格相关询问处理
        else if (message.contains("价格") || message.contains("费用") || message.contains("多少钱") || message.contains("收费")) {
            return "关于价格信息，我建议您联系我们的销售团队获取最新的报价。您可以提供具体需求，我们会为您制定合适的方案。";
        }
        // 联系方式询问处理
        else if (message.contains("联系") || message.contains("电话") || message.contains("客服")) {
            return "您可以通过以下方式联系我们：\n- 客服热线：400-123-4567\n- 邮箱：service@example.com\n- 工作时间：周一至周五 9:00-18:00";
        }
        // 技术问题处理
        else if (message.contains("问题") || message.contains("故障") || message.contains("不能") || message.contains("错误")) {
            return "我理解您遇到了技术问题。请详细描述一下具体的问题现象，我会尽力为您提供解决方案。如果问题复杂，我也可以为您转接技术支持专员。";
        }
        // 感谢类消息处理
        else if (message.contains("谢谢") || message.contains("感谢") || message.contains("thanks")) {
            return "不客气！很高兴能够帮助您。如果您还有其他问题，随时可以咨询我。";
        }
        // 告别类消息处理
        else if (message.contains("再见") || message.contains("拜拜") || message.contains("bye")) {
            return "再见！感谢您的咨询，祝您生活愉快！如有需要，欢迎随时联系我们。";
        }
        // 默认回复 - 处理未匹配到特定关键词的消息
        else {
            return String.format("感谢您的咨询！我已经收到您的问题：\"%s\"。我会尽力为您提供帮助。如需更详细的信息，建议您联系我们的专业客服团队。",
                message.length() > 50 ? message.substring(0, 50) + "..." : message);
        }
    }

    /**
     * 简化的聊天接口 - 直接处理字符串消息
     *
     * <p>这个方法提供了一个更简单的接口，直接接受字符串消息并返回字符串回复。
     * 内部会将字符串转换为Prompt对象，调用主要的call方法，然后提取文本回复。</p>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>简单的文本对话</li>
     *   <li>快速测试和原型开发</li>
     *   <li>不需要复杂上下文的场景</li>
     * </ul>
     *
     * @param message 用户输入的消息字符串
     * @return AI生成的回复字符串
     */
    @Override
    public String call(String message) {
        // 将字符串消息包装为Prompt对象
        Prompt prompt = new Prompt(message);

        // 调用主要的call方法处理
        ChatResponse response = call(prompt);

        // 提取并返回文本回复
        return response.getResult().getOutput().getText();
    }
}
