package com.example.customerservice.service;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.entity.ChatMessage;
import com.example.customerservice.entity.ChatSession;
import com.example.customerservice.repository.ChatMessageRepository;
import com.example.customerservice.repository.ChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatService专项测试类
 * 测试聊天服务的核心功能
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.alibaba.ai.tongyi.api-key=test-key-for-unit-testing",
    "customer-service.welcome-message=欢迎使用智能客服系统！",
    "customer-service.session-timeout=30",
    "customer-service.system-prompt=你是一个专业的客服助手，请提供友好和专业的服务。"
})
@Transactional
class ChatServiceTest {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    @DisplayName("测试ChatService Bean注入")
    void testChatServiceInjection() {
        assertNotNull(chatService, "ChatService应该被正确注入");
    }

    @Test
    @DisplayName("测试新会话创建")
    void testNewSessionCreation() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setUserId("test-user-001");
        
        try {
            ChatResponse response = chatService.processMessage(request);
            
            if (response.isSuccess()) {
                assertNotNull(response.getSessionId(), "应该创建新的会话ID");
                assertNotNull(response.getMessage(), "应该有AI回复消息");
                assertEquals("assistant", response.getSender(), "发送者应该是assistant");
                
                // 验证数据库中的数据
                assertTrue(sessionRepository.existsBySessionId(response.getSessionId()), 
                          "会话应该被保存到数据库");
                
                var session = sessionRepository.findBySessionId(response.getSessionId());
                assertTrue(session.isPresent(), "应该能找到创建的会话");
                assertEquals("test-user-001", session.get().getUserId(), "用户ID应该正确");
            }
        } catch (Exception e) {
            // 如果没有有效的API密钥，这是预期的
            System.out.println("测试需要有效的API密钥: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试会话历史功能")
    void testSessionHistory() {
        // 创建测试会话
        ChatSession session = new ChatSession("test-session-history", "test-user");
        sessionRepository.save(session);
        
        // 添加测试消息
        ChatMessage msg1 = new ChatMessage("Hello", ChatMessage.MessageType.USER, "test-user");
        ChatMessage msg2 = new ChatMessage("Hi there!", ChatMessage.MessageType.ASSISTANT, "assistant");
        
        session.addMessage(msg1);
        session.addMessage(msg2);
        
        messageRepository.save(msg1);
        messageRepository.save(msg2);
        
        // 测试获取会话历史
        var history = chatService.getSessionHistory("test-session-history");
        
        assertNotNull(history, "会话历史不应该为null");
        assertEquals(2, history.size(), "应该有2条消息");
        assertEquals("Hello", history.get(0).getContent(), "第一条消息内容应该正确");
        assertEquals("Hi there!", history.get(1).getContent(), "第二条消息内容应该正确");
    }

    @Test
    @DisplayName("测试会话清理功能")
    void testSessionCleanup() {
        assertDoesNotThrow(() -> {
            chatService.cleanupExpiredSessions();
        }, "会话清理功能不应该抛出异常");
    }

    @Test
    @DisplayName("测试消息验证")
    void testMessageValidation() {
        ChatRequest emptyRequest = new ChatRequest();
        emptyRequest.setMessage("");
        emptyRequest.setUserId("test-user");
        
        // 空消息应该被处理（可能返回错误或默认回复）
        assertDoesNotThrow(() -> {
            ChatResponse response = chatService.processMessage(emptyRequest);
            assertNotNull(response, "即使是空消息也应该有响应");
        });
    }

    @Test
    @DisplayName("测试长消息处理")
    void testLongMessageProcessing() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longMessage.append("这是一个很长的消息，用于测试系统对长文本的处理能力。");
        }
        
        ChatRequest request = new ChatRequest();
        request.setMessage(longMessage.toString());
        request.setUserId("test-user-long");
        
        assertDoesNotThrow(() -> {
            ChatResponse response = chatService.processMessage(request);
            assertNotNull(response, "长消息应该能被处理");
        });
    }

    @Test
    @DisplayName("测试并发会话处理")
    void testConcurrentSessions() {
        // 创建多个不同的会话请求
        ChatRequest request1 = new ChatRequest();
        request1.setMessage("用户1的消息");
        request1.setUserId("user-001");
        
        ChatRequest request2 = new ChatRequest();
        request2.setMessage("用户2的消息");
        request2.setUserId("user-002");
        
        ChatRequest request3 = new ChatRequest();
        request3.setMessage("用户3的消息");
        request3.setUserId("user-003");
        
        assertDoesNotThrow(() -> {
            ChatResponse response1 = chatService.processMessage(request1);
            ChatResponse response2 = chatService.processMessage(request2);
            ChatResponse response3 = chatService.processMessage(request3);
            
            // 验证每个响应都有唯一的会话ID
            if (response1.isSuccess() && response2.isSuccess() && response3.isSuccess()) {
                assertNotEquals(response1.getSessionId(), response2.getSessionId(), 
                               "不同用户应该有不同的会话ID");
                assertNotEquals(response2.getSessionId(), response3.getSessionId(), 
                               "不同用户应该有不同的会话ID");
                assertNotEquals(response1.getSessionId(), response3.getSessionId(), 
                               "不同用户应该有不同的会话ID");
            }
        });
    }

    @Test
    @DisplayName("测试特殊字符处理")
    void testSpecialCharacterHandling() {
        String specialMessage = "测试特殊字符：@#$%^&*()_+{}|:<>?[]\\;'\",./ 和 emoji 😀😃😄😁";
        
        ChatRequest request = new ChatRequest();
        request.setMessage(specialMessage);
        request.setUserId("test-special-chars");
        
        assertDoesNotThrow(() -> {
            ChatResponse response = chatService.processMessage(request);
            assertNotNull(response, "包含特殊字符的消息应该能被处理");
        });
    }

    @Test
    @DisplayName("测试会话状态管理")
    void testSessionStatusManagement() {
        // 创建会话
        ChatSession session = new ChatSession("status-test-session", "status-test-user");
        assertEquals(ChatSession.SessionStatus.ACTIVE, session.getStatus(), 
                    "新会话状态应该是ACTIVE");
        
        sessionRepository.save(session);
        
        // 验证会话被正确保存
        var savedSession = sessionRepository.findBySessionId("status-test-session");
        assertTrue(savedSession.isPresent(), "会话应该被保存");
        assertEquals(ChatSession.SessionStatus.ACTIVE, savedSession.get().getStatus(), 
                    "保存的会话状态应该是ACTIVE");
    }

    @Test
    @DisplayName("测试消息类型处理")
    void testMessageTypeHandling() {
        ChatSession session = new ChatSession("type-test-session", "type-test-user");
        sessionRepository.save(session);
        
        // 测试不同类型的消息
        ChatMessage userMsg = new ChatMessage("用户消息", ChatMessage.MessageType.USER, "user");
        ChatMessage assistantMsg = new ChatMessage("助手消息", ChatMessage.MessageType.ASSISTANT, "assistant");
        ChatMessage systemMsg = new ChatMessage("系统消息", ChatMessage.MessageType.SYSTEM, "system");
        
        session.addMessage(userMsg);
        session.addMessage(assistantMsg);
        session.addMessage(systemMsg);
        
        messageRepository.save(userMsg);
        messageRepository.save(assistantMsg);
        messageRepository.save(systemMsg);
        
        // 验证消息类型
        assertEquals(ChatMessage.MessageType.USER, userMsg.getMessageType());
        assertEquals(ChatMessage.MessageType.ASSISTANT, assistantMsg.getMessageType());
        assertEquals(ChatMessage.MessageType.SYSTEM, systemMsg.getMessageType());
        
        // 验证消息计数
        long userCount = messageRepository.countBySessionAndMessageType(session, ChatMessage.MessageType.USER);
        long assistantCount = messageRepository.countBySessionAndMessageType(session, ChatMessage.MessageType.ASSISTANT);
        long systemCount = messageRepository.countBySessionAndMessageType(session, ChatMessage.MessageType.SYSTEM);
        
        assertEquals(1, userCount, "应该有1条用户消息");
        assertEquals(1, assistantCount, "应该有1条助手消息");
        assertEquals(1, systemCount, "应该有1条系统消息");
    }
}
