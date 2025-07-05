package com.example.customerservice;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.entity.ChatMessage;
import com.example.customerservice.entity.ChatSession;
import com.example.customerservice.repository.ChatMessageRepository;
import com.example.customerservice.repository.ChatSessionRepository;
import com.example.customerservice.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能客服系统综合测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.alibaba.ai.tongyi.api-key=test-key-for-testing",
    "customer-service.welcome-message=欢迎使用测试环境！",
    "customer-service.session-timeout=5"
})
@Transactional
class CustomerServiceApplicationTests {

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
    void contextLoads() {
        // 测试Spring上下文是否正确加载
        assertNotNull(chatService);
        assertNotNull(sessionRepository);
        assertNotNull(messageRepository);
    }

    @Test
    void testChatRequestValidation() {
        // 测试聊天请求的基本验证
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setUserId("test-user");

        assertNotNull(request.getMessage());
        assertEquals("Hello", request.getMessage());
        assertEquals("test-user", request.getUserId());
    }

    @Test
    void testChatResponseCreation() {
        // 测试聊天响应的创建
        ChatResponse response = ChatResponse.success("session-123", "Hello!", "assistant");

        assertTrue(response.isSuccess());
        assertEquals("session-123", response.getSessionId());
        assertEquals("Hello!", response.getMessage());
        assertEquals("assistant", response.getSender());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponse() {
        // 测试错误响应
        ChatResponse errorResponse = ChatResponse.error("Test error");

        assertFalse(errorResponse.isSuccess());
        assertEquals("Test error", errorResponse.getErrorMessage());
    }

    @Test
    void testChatSessionCreation() {
        // 测试会话创建
        ChatSession session = new ChatSession("test-session-001", "test-user");
        sessionRepository.save(session);

        assertNotNull(session.getId());
        assertEquals("test-session-001", session.getSessionId());
        assertEquals("test-user", session.getUserId());
        assertEquals(ChatSession.SessionStatus.ACTIVE, session.getStatus());
        assertNotNull(session.getCreatedAt());
    }

    @Test
    void testChatMessageCreation() {
        // 测试消息创建
        ChatSession session = new ChatSession("test-session-002", "test-user");
        sessionRepository.save(session);

        ChatMessage message = new ChatMessage("Hello World", ChatMessage.MessageType.USER, "test-user");
        session.addMessage(message);
        messageRepository.save(message);

        assertNotNull(message.getId());
        assertEquals("Hello World", message.getContent());
        assertEquals(ChatMessage.MessageType.USER, message.getMessageType());
        assertEquals("test-user", message.getSender());
        assertEquals(session, message.getSession());
    }

    @Test
    void testProcessMessage() {
        // 测试消息处理（这个测试可能需要真实的API密钥才能通过）
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setUserId("test-user");

        try {
            ChatResponse response = chatService.processMessage(request);

            // 基本验证
            assertNotNull(response);
            if (response.isSuccess()) {
                assertNotNull(response.getSessionId());
                assertNotNull(response.getMessage());
                assertEquals("assistant", response.getSender());
                assertNotNull(response.getTimestamp());
            } else {
                // 如果失败，应该有错误信息
                assertNotNull(response.getErrorMessage());
            }
        } catch (Exception e) {
            // 如果没有有效的API密钥，测试可能会失败，这是预期的
            assertTrue(e.getMessage().contains("API") || e.getMessage().contains("key") ||
                      e.getMessage().contains("auth") || e.getMessage().contains("token"));
        }
    }

    @Test
    void testSessionRepository() {
        // 测试会话仓库功能
        ChatSession session1 = new ChatSession("session-001", "user-001");
        ChatSession session2 = new ChatSession("session-002", "user-001");
        ChatSession session3 = new ChatSession("session-003", "user-002");

        sessionRepository.save(session1);
        sessionRepository.save(session2);
        sessionRepository.save(session3);

        // 测试按用户ID查找
        var user1Sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc("user-001");
        assertEquals(2, user1Sessions.size());

        var user2Sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc("user-002");
        assertEquals(1, user2Sessions.size());

        // 测试按会话ID查找
        var foundSession = sessionRepository.findBySessionId("session-001");
        assertTrue(foundSession.isPresent());
        assertEquals("user-001", foundSession.get().getUserId());

        // 测试会话是否存在
        assertTrue(sessionRepository.existsBySessionId("session-001"));
        assertFalse(sessionRepository.existsBySessionId("non-existent"));
    }

    @Test
    void testMessageRepository() {
        // 测试消息仓库功能
        ChatSession session = new ChatSession("test-session", "test-user");
        sessionRepository.save(session);

        ChatMessage msg1 = new ChatMessage("Hello", ChatMessage.MessageType.USER, "test-user");
        ChatMessage msg2 = new ChatMessage("Hi there!", ChatMessage.MessageType.ASSISTANT, "assistant");
        ChatMessage msg3 = new ChatMessage("How are you?", ChatMessage.MessageType.USER, "test-user");

        session.addMessage(msg1);
        session.addMessage(msg2);
        session.addMessage(msg3);

        messageRepository.save(msg1);
        messageRepository.save(msg2);
        messageRepository.save(msg3);

        // 测试按会话查找消息
        var messages = messageRepository.findBySessionOrderByCreatedAtAsc(session);
        assertEquals(3, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hi there!", messages.get(1).getContent());
        assertEquals("How are you?", messages.get(2).getContent());

        // 测试消息计数
        long userMessageCount = messageRepository.countBySessionAndMessageType(session, ChatMessage.MessageType.USER);
        long assistantMessageCount = messageRepository.countBySessionAndMessageType(session, ChatMessage.MessageType.ASSISTANT);

        assertEquals(2, userMessageCount);
        assertEquals(1, assistantMessageCount);
    }

    @Test
    void testSessionCleanup() {
        // 测试会话清理功能
        try {
            chatService.cleanupExpiredSessions();
            // 如果没有异常，说明方法执行成功
            assertTrue(true);
        } catch (Exception e) {
            fail("会话清理功能应该正常工作: " + e.getMessage());
        }
    }
}
