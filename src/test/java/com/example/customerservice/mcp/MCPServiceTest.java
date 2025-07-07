package com.example.customerservice.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MCPService单元测试
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MCPServiceTest {
    
    private MCPService mcpService;
    
    @Mock
    private MCPMessageHandler mockHandler;
    
    @BeforeEach
    void setUp() {
        mcpService = new MCPService();
    }
    
    @Test
    void testRegisterHandler() {
        // 注册处理器
        mcpService.registerHandler(MCPMessageType.CONTEXT_REQUEST, mockHandler);
        
        // 验证处理器已注册（通过处理消息来验证）
        MCPMessage message = MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source("test-source")
                .target("test-target")
                .build();
        
        MCPMessage expectedResponse = message.createResponse(MCPMessageType.CONTEXT_RESPONSE, "test response");
        when(mockHandler.handle(eq(message), eq(mcpService))).thenReturn(expectedResponse);
        
        CompletableFuture<MCPMessage> responseFuture = mcpService.handleMessage(message);
        MCPMessage response = responseFuture.join();
        
        assertEquals(expectedResponse, response);
        verify(mockHandler).handle(eq(message), eq(mcpService));
    }
    
    @Test
    void testHandleMessageWithNoHandler() {
        // 创建没有处理器的消息
        MCPMessage message = MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source("test-source")
                .target("test-target")
                .build();
        
        // 处理消息
        CompletableFuture<MCPMessage> responseFuture = mcpService.handleMessage(message);
        MCPMessage response = responseFuture.join();
        
        // 验证返回错误响应
        assertEquals(MCPMessageType.ERROR, response.getType());
        assertTrue(response.getMetadata().values().contains("No handler found"));
    }
    
    @Test
    void testHandleExpiredMessage() {
        // 创建已过期的消息
        MCPMessage message = MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source("test-source")
                .target("test-target")
                .ttl(1) // 1毫秒TTL
                .build();
        
        // 等待消息过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 处理过期消息
        CompletableFuture<MCPMessage> responseFuture = mcpService.handleMessage(message);
        MCPMessage response = responseFuture.join();
        
        // 验证返回过期错误
        assertEquals(MCPMessageType.ERROR, response.getType());
        assertTrue(response.getMetadata().values().contains("expired"));
    }
    
    @Test
    void testHandleMessageWithException() {
        // 注册会抛出异常的处理器
        mcpService.registerHandler(MCPMessageType.CONTEXT_REQUEST, mockHandler);
        
        MCPMessage message = MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source("test-source")
                .target("test-target")
                .build();
        
        when(mockHandler.handle(any(), any())).thenThrow(new RuntimeException("Test exception"));
        
        // 处理消息
        CompletableFuture<MCPMessage> responseFuture = mcpService.handleMessage(message);
        MCPMessage response = responseFuture.join();
        
        // 验证返回错误响应
        assertEquals(MCPMessageType.ERROR, response.getType());
        assertTrue(response.getMetadata().values().contains("Internal error"));
    }
    
    @Test
    void testGetOrCreateContext() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 第一次调用应该创建新上下文
        MCPContext context1 = mcpService.getOrCreateContext(sessionId, userId);
        assertNotNull(context1);
        assertEquals(sessionId, context1.getSessionId());
        assertEquals(userId, context1.getUserId());
        
        // 第二次调用应该返回相同的上下文
        MCPContext context2 = mcpService.getOrCreateContext(sessionId, userId);
        assertSame(context1, context2);
    }
    
    @Test
    void testGetContext() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 获取不存在的上下文
        MCPContext context = mcpService.getContext(sessionId);
        assertNull(context);
        
        // 创建上下文后再获取
        mcpService.getOrCreateContext(sessionId, userId);
        context = mcpService.getContext(sessionId);
        assertNotNull(context);
        assertEquals(sessionId, context.getSessionId());
    }
    
    @Test
    void testUpdateContext() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 创建上下文
        MCPContext context = mcpService.getOrCreateContext(sessionId, userId);
        context.setCurrentIntent("test-intent");
        
        // 更新上下文
        mcpService.updateContext(context);
        
        // 验证上下文已更新
        MCPContext retrievedContext = mcpService.getContext(sessionId);
        assertEquals("test-intent", retrievedContext.getCurrentIntent());
    }
    
    @Test
    void testRemoveContext() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 创建上下文
        mcpService.getOrCreateContext(sessionId, userId);
        assertNotNull(mcpService.getContext(sessionId));
        
        // 删除上下文
        mcpService.removeContext(sessionId);
        assertNull(mcpService.getContext(sessionId));
    }
    
    @Test
    void testShareContext() {
        String fromSessionId = "session-1";
        String toSessionId = "session-2";
        String userId = "test-user";
        
        // 创建源上下文
        MCPContext sourceContext = mcpService.getOrCreateContext(fromSessionId, userId);
        sourceContext.setCurrentIntent("shared-intent");
        sourceContext.addConversationMessage("user", "test message");
        
        // 共享上下文
        boolean result = mcpService.shareContext(fromSessionId, toSessionId);
        assertTrue(result);
        
        // 验证目标上下文已创建并包含共享数据
        MCPContext targetContext = mcpService.getContext(toSessionId);
        assertNotNull(targetContext);
        assertEquals(toSessionId, targetContext.getSessionId());
        assertEquals("shared-intent", targetContext.getCurrentIntent());
        assertEquals(1, targetContext.getConversationHistory().size());
    }
    
    @Test
    void testShareContextWithNonExistentSource() {
        String fromSessionId = "non-existent";
        String toSessionId = "session-2";
        
        // 尝试共享不存在的上下文
        boolean result = mcpService.shareContext(fromSessionId, toSessionId);
        assertFalse(result);
        
        // 验证目标上下文未创建
        assertNull(mcpService.getContext(toSessionId));
    }
    
    @Test
    void testBroadcastMessage() {
        List<String> targetSessions = List.of("session-1", "session-2", "session-3");
        
        MCPMessage message = MCPMessage.builder()
                .type(MCPMessageType.STATUS_UPDATE)
                .source("broadcast-source")
                .payload(Map.of("status", "active"))
                .build();
        
        // 广播消息
        List<CompletableFuture<Boolean>> results = mcpService.broadcastMessage(message, targetSessions);
        
        // 验证返回的Future数量
        assertEquals(3, results.size());
        
        // 验证所有消息都发送成功（在当前实现中总是返回true）
        results.forEach(future -> assertTrue(future.join()));
    }
    
    @Test
    void testCreateContextRequest() {
        String source = "test-source";
        String target = "test-target";
        String sessionId = "test-session";
        
        MCPMessage request = mcpService.createContextRequest(source, target, sessionId);
        
        assertNotNull(request);
        assertEquals(MCPMessageType.CONTEXT_REQUEST, request.getType());
        assertEquals(source, request.getSource());
        assertEquals(target, request.getTarget());
        assertEquals(sessionId, request.getSessionId());
    }
    
    @Test
    void testCreateContextResponse() {
        MCPMessage request = MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source("test-source")
                .target("test-target")
                .sessionId("test-session")
                .build();
        
        MCPContext context = new MCPContext("test-session", "test-user");
        
        MCPMessage response = mcpService.createContextResponse(request, context);
        
        assertNotNull(response);
        assertEquals(MCPMessageType.CONTEXT_RESPONSE, response.getType());
        assertEquals(request.getTarget(), response.getSource());
        assertEquals(request.getSource(), response.getTarget());
        assertEquals(request.getId(), response.getCorrelationId());
        assertEquals(context, response.getPayload());
    }
    
    @Test
    void testCreateStatusUpdate() {
        String source = "test-source";
        String sessionId = "test-session";
        Map<String, Object> status = Map.of("state", "active", "load", 0.5);
        
        MCPMessage statusUpdate = mcpService.createStatusUpdate(source, sessionId, status);
        
        assertNotNull(statusUpdate);
        assertEquals(MCPMessageType.STATUS_UPDATE, statusUpdate.getType());
        assertEquals(source, statusUpdate.getSource());
        assertEquals(sessionId, statusUpdate.getSessionId());
        assertEquals(status, statusUpdate.getPayload());
    }
    
    @Test
    void testCreateHeartbeat() {
        String source = "test-source";
        
        MCPMessage heartbeat = mcpService.createHeartbeat(source);
        
        assertNotNull(heartbeat);
        assertEquals(MCPMessageType.HEARTBEAT, heartbeat.getType());
        assertEquals(source, heartbeat.getSource());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) heartbeat.getPayload();
        assertTrue(payload.containsKey("timestamp"));
    }
    
    @Test
    void testGetStatistics() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 创建一些上下文和处理器
        mcpService.getOrCreateContext(sessionId, userId);
        mcpService.registerHandler(MCPMessageType.CONTEXT_REQUEST, mockHandler);
        
        Map<String, Object> stats = mcpService.getStatistics();
        
        assertNotNull(stats);
        assertEquals(1, stats.get("activeContexts"));
        assertEquals(1, stats.get("registeredHandlers"));
        assertTrue(stats.containsKey("handlerTypes"));
    }
    
    @Test
    void testCleanupExpiredContexts() {
        String sessionId1 = "session-1";
        String sessionId2 = "session-2";
        String userId = "test-user";
        
        // 创建上下文
        MCPContext context1 = mcpService.getOrCreateContext(sessionId1, userId);
        MCPContext context2 = mcpService.getOrCreateContext(sessionId2, userId);
        
        // 手动设置一个上下文为过期
        context1.setUpdatedAt(java.time.LocalDateTime.now().minusHours(2));
        mcpService.updateContext(context1);
        
        // 清理过期上下文（1小时过期）
        int cleanedCount = mcpService.cleanupExpiredContexts(60);
        
        // 验证清理结果
        assertEquals(1, cleanedCount);
        assertNull(mcpService.getContext(sessionId1));
        assertNotNull(mcpService.getContext(sessionId2));
    }
    
    @Test
    void testIsHealthy() {
        assertTrue(mcpService.isHealthy());
    }
    
    @Test
    void testShutdown() {
        String sessionId = "test-session";
        String userId = "test-user";
        
        // 创建一些数据
        mcpService.getOrCreateContext(sessionId, userId);
        mcpService.registerHandler(MCPMessageType.CONTEXT_REQUEST, mockHandler);
        
        // 关闭服务
        mcpService.shutdown();
        
        // 验证数据已清理
        Map<String, Object> stats = mcpService.getStatistics();
        assertEquals(0, stats.get("activeContexts"));
        assertEquals(0, stats.get("registeredHandlers"));
    }
}
