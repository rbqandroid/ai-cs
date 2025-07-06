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
import org.springframework.beans.factory.annotation.Autowired;
import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.service.KnowledgeSearchService;
import com.example.customerservice.service.RAGService;
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
 *
 * 负责处理用户消息、AI响应和会话管理的核心业务逻辑。
 * 集成了知识库搜索功能，能够基于知识库内容提供更准确的AI回答。
 *
 * 主要功能：
 * - 处理用户聊天消息
 * - 管理聊天会话生命周期
 * - 集成AI模型生成智能回复
 * - 搜索知识库提供上下文信息
 * - 记录和分析对话历史
 * - 自动清理过期会话
 *
 * @author AI Assistant
 * @version 2.0.0
 * @since 2025-07-06
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

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    @Autowired
    private RAGService ragService;

    @Value("${customer-service.welcome-message}")
    private String welcomeMessage;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
    @Value("${customer-service.system-prompt}")
    private String systemPrompt;
    
    @Value("${customer-service.session-timeout:30}")
    private int sessionTimeoutMinutes;
    
    /**
     * 处理聊天消息的核心方法
     *
     * 该方法执行以下步骤：
     * 1. 获取或创建用户会话
     * 2. 保存用户消息到数据库
     * 3. 构建包含知识库内容的对话上下文
     * 4. 调用AI模型生成回复
     * 5. 保存AI回复到数据库
     * 6. 更新会话状态
     *
     * @param request 聊天请求对象，包含会话ID、用户ID和消息内容
     * @return ChatResponse 聊天响应对象，包含AI回复和相关元数据
     * @throws RuntimeException 当处理过程中发生错误时抛出
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
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);

            String aiReply = aiResponse.getResult().getOutput().getText();

            // 保存AI响应
            ChatMessage assistantMessage = new ChatMessage(
                aiReply,
                ChatMessage.MessageType.ASSISTANT,
                "assistant"
            );

            // 暂时设置固定token数量
            assistantMessage.setTokensUsed(50);
            
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
     * 构建增强的系统提示，包含会话上下文信息和RAG增强内容
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

        // 获取最近的用户消息，用于RAG增强
        String latestUserMessage = getLatestUserMessage(session);
        if (latestUserMessage != null && !latestUserMessage.trim().isEmpty()) {
            try {
                if (ragEnabled) {
                    // 使用RAG服务构建增强提示
                    String enhancedPrompt = ragService.buildEnhancedPrompt(latestUserMessage, promptBuilder.toString());
                    return enhancedPrompt + buildSessionContext(messageCount);
                } else {
                    // 回退到传统知识库搜索
                    List<KnowledgeDocument> relevantDocs = knowledgeSearchService.searchByKeyword(latestUserMessage, 3);

                    if (!relevantDocs.isEmpty()) {
                        promptBuilder.append("\n\n相关知识库内容：");
                        for (int i = 0; i < relevantDocs.size(); i++) {
                            KnowledgeDocument doc = relevantDocs.get(i);
                            promptBuilder.append(String.format("\n%d. 标题：%s", i + 1, doc.getTitle()));
                            if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
                                promptBuilder.append(String.format("\n   摘要：%s", doc.getSummary()));
                            }

                            // 如果内容不太长，添加部分内容
                            if (doc.getContent() != null) {
                                String content = doc.getContent().length() <= 300 ?
                                    doc.getContent() : doc.getContent().substring(0, 300) + "...";
                                promptBuilder.append(String.format("\n   内容：%s", content));
                            }
                        }
                        promptBuilder.append("\n\n请优先基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请诚实说明并提供一般性建议。");
                    }
                }
            } catch (Exception e) {
                logger.warn("RAG增强失败，回退到基础模式: {}", e.getMessage());
                // RAG失败不影响正常对话
            }
        }

        promptBuilder.append(buildSessionContext(messageCount));
        return promptBuilder.toString();
    }

    /**
     * 构建会话上下文信息
     *
     * @param messageCount 消息数量
     * @return 会话上下文字符串
     */
    private String buildSessionContext(long messageCount) {
        StringBuilder contextBuilder = new StringBuilder();

        // 添加用户偏好和历史行为分析
        if (messageCount > 0) {
            contextBuilder.append("\n\n用户行为分析：");
            contextBuilder.append("\n- 这是一个").append(messageCount > 5 ? "活跃" : "新").append("用户");
            contextBuilder.append("\n- 请根据对话历史提供个性化服务");
        }

        contextBuilder.append("\n\n请基于以上信息和知识库内容提供专业、友好、个性化的客服服务。");

        return contextBuilder.toString();
    }

    /**
     * 获取最近的用户消息
     *
     * @param session 聊天会话
     * @return 最近的用户消息内容
     */
    private String getLatestUserMessage(ChatSession session) {
        // 从数据库获取最近的用户消息
        List<ChatMessage> recentMessages = messageRepository.findRecentMessagesBySession(
            session, org.springframework.data.domain.PageRequest.of(0, 5)
        );

        // 查找最近的用户消息
        for (ChatMessage message : recentMessages) {
            if (message.getMessageType() == ChatMessage.MessageType.USER) {
                return message.getContent();
            }
        }

        return null;
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
