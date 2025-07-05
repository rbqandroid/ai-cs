package com.example.customerservice.service;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.entity.ChatMessage;
import com.example.customerservice.entity.ChatSession;
import com.example.customerservice.repository.ChatMessageRepository;
import com.example.customerservice.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 聊天服务核心类
 * 负责处理用户消息、AI响应和会话管理
 */
@Service
@Transactional
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Value("${customer-service.welcome-message}")
    private String welcomeMessage;
    
    @Value("${customer-service.system-prompt}")
    private String systemPrompt;
    
    @Value("${customer-service.session-timeout:30}")
    private int sessionTimeoutMinutes;
    
    /**
     * 处理聊天消息
     */
    public com.example.customerservice.dto.ChatResponse processMessage(ChatRequest request) {
        try {
            logger.info("处理聊天消息: {}", request);
            
            // 获取或创建会话
            ChatSession session = getOrCreateSession(request.getSessionId(), request.getUserId());
            
            // 保存用户消息
            ChatMessage userMessage = new ChatMessage(
                request.getMessage(), 
                ChatMessage.MessageType.USER, 
                request.getUserId() != null ? request.getUserId() : "anonymous"
            );
            session.addMessage(userMessage);
            messageRepository.save(userMessage);
            
            // 构建对话上下文
            List<Message> messages = buildConversationContext(session);

            // 调用AI获取响应
            Prompt prompt = new Prompt(messages);
            ChatResponse aiResponse = chatModel.call(prompt);

            String aiReply = aiResponse.getResult().getOutput().getContent();
            
            // 保存AI响应
            ChatMessage assistantMessage = new ChatMessage(
                aiReply,
                ChatMessage.MessageType.ASSISTANT,
                "assistant"
            );
            
            // 设置token使用量（如果可用）
            if (aiResponse.getMetadata() != null && aiResponse.getMetadata().getUsage() != null) {
                assistantMessage.setTokensUsed(aiResponse.getMetadata().getUsage().getTotalTokens());
            } else {
                // 简单估算
                assistantMessage.setTokensUsed(aiReply.length() / 4);
            }
            
            session.addMessage(assistantMessage);
            messageRepository.save(assistantMessage);
            sessionRepository.save(session);
            
            // 构建响应
            com.example.customerservice.dto.ChatResponse response = 
                com.example.customerservice.dto.ChatResponse.success(
                    session.getSessionId(), 
                    aiReply, 
                    "assistant"
                );
            response.setTokensUsed(assistantMessage.getTokensUsed());
            
            logger.info("成功处理聊天消息，会话ID: {}", session.getSessionId());
            return response;
            
        } catch (Exception e) {
            logger.error("处理聊天消息时发生错误", e);
            return com.example.customerservice.dto.ChatResponse.error("处理消息时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(String sessionId, String userId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            Optional<ChatSession> existingSession = sessionRepository.findBySessionId(sessionId);
            if (existingSession.isPresent()) {
                ChatSession session = existingSession.get();
                // 检查会话是否过期
                if (isSessionExpired(session)) {
                    session.setStatus(ChatSession.SessionStatus.INACTIVE);
                    sessionRepository.save(session);
                    // 创建新会话
                    return createNewSession(userId);
                }
                return session;
            }
        }
        
        return createNewSession(userId);
    }
    
    /**
     * 创建新会话
     */
    private ChatSession createNewSession(String userId) {
        String newSessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(newSessionId, userId);
        
        // 添加欢迎消息
        ChatMessage welcomeMsg = new ChatMessage(
            welcomeMessage,
            ChatMessage.MessageType.SYSTEM,
            "system"
        );
        session.addMessage(welcomeMsg);
        
        sessionRepository.save(session);
        messageRepository.save(welcomeMsg);
        
        logger.info("创建新会话: {}", newSessionId);
        return session;
    }
    
    /**
     * 构建对话上下文
     */
    private List<Message> buildConversationContext(ChatSession session) {
        List<Message> messages = new ArrayList<>();

        // 添加系统提示
        String enhancedSystemPrompt = buildEnhancedSystemPrompt(session);
        messages.add(new SystemMessage(enhancedSystemPrompt));

        // 获取最近的对话历史（限制数量以控制token使用）
        List<ChatMessage> recentMessages = messageRepository.findRecentMessagesBySession(
            session,
            org.springframework.data.domain.PageRequest.of(0, 10)
        );

        // 反转顺序，使其按时间正序
        java.util.Collections.reverse(recentMessages);

        // 转换为AI消息格式
        for (ChatMessage msg : recentMessages) {
            if (msg.getMessageType() == ChatMessage.MessageType.USER) {
                messages.add(new UserMessage(msg.getContent()));
            }
            // 注意：这里不添加ASSISTANT消息到上下文中，让AI基于用户输入生成新回复
        }

        return messages;
    }

    /**
     * 生成模拟AI回复（临时方案）
     */
    private String generateMockAiResponse(String userMessage) {
        String message = userMessage.toLowerCase();

        // 简单的关键词匹配回复
        if (message.contains("你好") || message.contains("hello")) {
            return "您好！我是智能客服助手，很高兴为您服务。请问有什么可以帮助您的吗？";
        } else if (message.contains("产品") || message.contains("服务")) {
            return "我们提供多种优质的产品和服务。请告诉我您具体想了解哪方面的信息，我会为您详细介绍。";
        } else if (message.contains("价格") || message.contains("费用") || message.contains("多少钱")) {
            return "关于价格信息，我建议您联系我们的销售团队获取最新的报价。您可以提供具体需求，我们会为您制定合适的方案。";
        } else if (message.contains("联系") || message.contains("电话") || message.contains("客服")) {
            return "您可以通过以下方式联系我们：\n- 客服热线：400-123-4567\n- 邮箱：service@example.com\n- 工作时间：周一至周五 9:00-18:00";
        } else if (message.contains("谢谢") || message.contains("感谢")) {
            return "不客气！很高兴能够帮助您。如果您还有其他问题，随时可以咨询我。";
        } else if (message.contains("再见") || message.contains("拜拜")) {
            return "再见！感谢您的咨询，祝您生活愉快！如有需要，欢迎随时联系我们。";
        } else {
            return "感谢您的咨询！我已经收到您的问题：\"" + userMessage + "\"。我会尽力为您提供帮助。如需更详细的信息，建议您联系我们的专业客服团队。";
        }
    }

    /**
     * 构建增强的系统提示，包含会话上下文信息
     */
    private String buildEnhancedSystemPrompt(ChatSession session) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(systemPrompt);

        // 添加会话信息
        promptBuilder.append("\n\n会话信息：");
        promptBuilder.append("\n- 会话ID: ").append(session.getSessionId());
        promptBuilder.append("\n- 用户ID: ").append(session.getUserId() != null ? session.getUserId() : "匿名用户");
        promptBuilder.append("\n- 会话开始时间: ").append(session.getCreatedAt());

        // 添加消息统计
        long messageCount = messageRepository.countBySession(session);
        promptBuilder.append("\n- 当前对话轮数: ").append(messageCount);

        // 添加用户偏好和历史行为分析
        if (messageCount > 0) {
            promptBuilder.append("\n\n用户行为分析：");
            promptBuilder.append("\n- 这是一个").append(messageCount > 5 ? "活跃" : "新").append("用户");
            promptBuilder.append("\n- 请根据对话历史提供个性化服务");
        }

        promptBuilder.append("\n\n请基于以上信息提供专业、友好、个性化的客服服务。");

        return promptBuilder.toString();
    }

    /**
     * 分析用户意图（高级功能）
     */
    private String analyzeUserIntent(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("价格") || lowerMessage.contains("费用") || lowerMessage.contains("多少钱")) {
            return "价格咨询";
        } else if (lowerMessage.contains("产品") || lowerMessage.contains("功能") || lowerMessage.contains("介绍")) {
            return "产品咨询";
        } else if (lowerMessage.contains("问题") || lowerMessage.contains("故障") || lowerMessage.contains("不能")) {
            return "技术支持";
        } else if (lowerMessage.contains("投诉") || lowerMessage.contains("不满") || lowerMessage.contains("差")) {
            return "投诉处理";
        } else if (lowerMessage.contains("退款") || lowerMessage.contains("退货") || lowerMessage.contains("取消")) {
            return "售后服务";
        } else {
            return "一般咨询";
        }
    }
    
    /**
     * 检查会话是否过期
     */
    private boolean isSessionExpired(ChatSession session) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        return session.getUpdatedAt().isBefore(cutoffTime);
    }
    
    /**
     * 获取会话历史
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getSessionHistory(String sessionId) {
        Optional<ChatSession> session = sessionRepository.findBySessionId(sessionId);
        if (session.isPresent()) {
            return messageRepository.findBySessionOrderByCreatedAtAsc(session.get());
        }
        return new ArrayList<>();
    }
    
    /**
     * 清理过期会话
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        List<ChatSession> expiredSessions = sessionRepository.findSessionsOlderThan(
            cutoffTime, 
            ChatSession.SessionStatus.ACTIVE
        );
        
        for (ChatSession session : expiredSessions) {
            session.setStatus(ChatSession.SessionStatus.INACTIVE);
            sessionRepository.save(session);
        }
        
        logger.info("清理了 {} 个过期会话", expiredSessions.size());
    }
}
