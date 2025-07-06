package com.example.customerservice.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP服务
 * 负责MCP消息的处理和上下文管理
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@Service
public class MCPService {
    
    private static final Logger logger = LoggerFactory.getLogger(MCPService.class);
    
    private final Map<String, MCPContext> contexts = new ConcurrentHashMap<>();
    private final Map<String, MCPMessageHandler> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 注册消息处理器
     * 
     * @param messageType 消息类型
     * @param handler 处理器
     */
    public void registerHandler(MCPMessageType messageType, MCPMessageHandler handler) {
        handlers.put(messageType.getCode(), handler);
        logger.info("Registered MCP handler for message type: {}", messageType.getCode());
    }
    
    /**
     * 处理MCP消息
     * 
     * @param message MCP消息
     * @return 处理结果的Future
     */
    public CompletableFuture<MCPMessage> handleMessage(MCPMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Handling MCP message: {}", message);
                
                // 检查消息是否过期
                if (message.isExpired()) {
                    return message.createErrorResponse("Message expired", "MESSAGE_EXPIRED");
                }
                
                // 获取处理器
                MCPMessageHandler handler = handlers.get(message.getType().getCode());
                if (handler == null) {
                    return message.createErrorResponse("No handler found for message type: " + message.getType(), "NO_HANDLER");
                }
                
                // 执行处理
                MCPMessage response = handler.handle(message, this);
                logger.debug("MCP message handled successfully: {}", message.getId());
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error handling MCP message: {}", message.getId(), e);
                return message.createErrorResponse("Internal error: " + e.getMessage(), "INTERNAL_ERROR");
            }
        }, executorService);
    }
    
    /**
     * 发送MCP消息
     * 
     * @param message 消息
     * @return 发送结果的Future
     */
    public CompletableFuture<Boolean> sendMessage(MCPMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Sending MCP message: {}", message);
                
                // 这里可以实现实际的消息发送逻辑
                // 例如通过WebSocket、HTTP、消息队列等方式发送
                
                // 目前只是模拟发送成功
                logger.info("MCP message sent successfully: {}", message.getId());
                return true;
                
            } catch (Exception e) {
                logger.error("Error sending MCP message: {}", message.getId(), e);
                return false;
            }
        }, executorService);
    }
    
    /**
     * 获取或创建上下文
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return MCP上下文
     */
    public MCPContext getOrCreateContext(String sessionId, String userId) {
        return contexts.computeIfAbsent(sessionId, k -> {
            MCPContext context = new MCPContext(sessionId, userId);
            logger.debug("Created new MCP context for session: {}", sessionId);
            return context;
        });
    }
    
    /**
     * 获取上下文
     * 
     * @param sessionId 会话ID
     * @return MCP上下文
     */
    public MCPContext getContext(String sessionId) {
        return contexts.get(sessionId);
    }
    
    /**
     * 更新上下文
     * 
     * @param context 上下文
     */
    public void updateContext(MCPContext context) {
        if (context != null && context.getSessionId() != null) {
            contexts.put(context.getSessionId(), context);
            logger.debug("Updated MCP context for session: {}", context.getSessionId());
        }
    }
    
    /**
     * 删除上下文
     * 
     * @param sessionId 会话ID
     */
    public void removeContext(String sessionId) {
        MCPContext removed = contexts.remove(sessionId);
        if (removed != null) {
            logger.debug("Removed MCP context for session: {}", sessionId);
        }
    }
    
    /**
     * 共享上下文
     * 
     * @param fromSessionId 源会话ID
     * @param toSessionId 目标会话ID
     * @return 是否成功
     */
    public boolean shareContext(String fromSessionId, String toSessionId) {
        MCPContext sourceContext = contexts.get(fromSessionId);
        if (sourceContext == null) {
            logger.warn("Source context not found for session: {}", fromSessionId);
            return false;
        }
        
        MCPContext sharedContext = sourceContext.copy();
        sharedContext.setSessionId(toSessionId);
        contexts.put(toSessionId, sharedContext);
        
        logger.info("Shared context from session {} to session {}", fromSessionId, toSessionId);
        return true;
    }
    
    /**
     * 广播消息
     * 
     * @param message 消息
     * @param targetSessions 目标会话列表
     * @return 发送结果的Future列表
     */
    public java.util.List<CompletableFuture<Boolean>> broadcastMessage(MCPMessage message, java.util.List<String> targetSessions) {
        return targetSessions.stream()
                .map(sessionId -> {
                    MCPMessage copy = MCPMessage.builder()
                            .type(message.getType())
                            .source(message.getSource())
                            .target(sessionId)
                            .sessionId(sessionId)
                            .payload(message.getPayload())
                            .metadata(message.getMetadata())
                            .priority(message.getPriority())
                            .build();
                    return sendMessage(copy);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 创建上下文请求消息
     * 
     * @param source 源
     * @param target 目标
     * @param sessionId 会话ID
     * @return 上下文请求消息
     */
    public MCPMessage createContextRequest(String source, String target, String sessionId) {
        return MCPMessage.builder()
                .type(MCPMessageType.CONTEXT_REQUEST)
                .source(source)
                .target(target)
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * 创建上下文响应消息
     * 
     * @param request 请求消息
     * @param context 上下文
     * @return 上下文响应消息
     */
    public MCPMessage createContextResponse(MCPMessage request, MCPContext context) {
        return request.createResponse(MCPMessageType.CONTEXT_RESPONSE, context);
    }
    
    /**
     * 创建状态更新消息
     * 
     * @param source 源
     * @param sessionId 会话ID
     * @param status 状态信息
     * @return 状态更新消息
     */
    public MCPMessage createStatusUpdate(String source, String sessionId, Map<String, Object> status) {
        return MCPMessage.builder()
                .type(MCPMessageType.STATUS_UPDATE)
                .source(source)
                .sessionId(sessionId)
                .payload(status)
                .build();
    }
    
    /**
     * 创建心跳消息
     * 
     * @param source 源
     * @return 心跳消息
     */
    public MCPMessage createHeartbeat(String source) {
        return MCPMessage.builder()
                .type(MCPMessageType.HEARTBEAT)
                .source(source)
                .payload(Map.of("timestamp", System.currentTimeMillis()))
                .build();
    }
    
    /**
     * 获取统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeContexts", contexts.size());
        stats.put("registeredHandlers", handlers.size());
        stats.put("handlerTypes", handlers.keySet());
        return stats;
    }
    
    /**
     * 清理过期上下文
     * 
     * @param maxAgeMinutes 最大年龄（分钟）
     * @return 清理的上下文数量
     */
    public int cleanupExpiredContexts(long maxAgeMinutes) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(maxAgeMinutes);
        
        java.util.List<String> expiredSessions = contexts.entrySet().stream()
                .filter(entry -> entry.getValue().getUpdatedAt().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        expiredSessions.forEach(this::removeContext);
        
        logger.info("Cleaned up {} expired MCP contexts", expiredSessions.size());
        return expiredSessions.size();
    }
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    public boolean isHealthy() {
        return !executorService.isShutdown() && !executorService.isTerminated();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
        contexts.clear();
        handlers.clear();
        logger.info("MCP service shutdown completed");
    }
}
