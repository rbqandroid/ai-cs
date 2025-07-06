package com.example.customerservice.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * MCP (Model Context Protocol) 消息定义
 * 用于模型间通信和上下文共享
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPMessage {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private MCPMessageType type;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("target")
    private String target;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("payload")
    private Object payload;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("priority")
    private int priority = 5; // 默认优先级
    
    @JsonProperty("ttl")
    private long ttl; // 生存时间（毫秒）
    
    @JsonProperty("correlationId")
    private String correlationId; // 关联ID，用于请求-响应匹配
    
    public MCPMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    public MCPMessage(MCPMessageType type, String source, String target, Object payload) {
        this();
        this.type = type;
        this.source = source;
        this.target = target;
        this.payload = payload;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public MCPMessageType getType() { return type; }
    public void setType(MCPMessageType type) { this.type = type; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = Math.max(1, Math.min(10, priority)); }
    
    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    /**
     * 获取载荷并转换为指定类型
     * 
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 转换后的载荷
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        if (clazz.isInstance(payload)) {
            return (T) payload;
        }
        throw new ClassCastException("Cannot cast payload to " + clazz.getName());
    }
    
    /**
     * 获取元数据值
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 元数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return metadata != null ? (T) metadata.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置元数据值
     * 
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 检查消息是否已过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        if (ttl <= 0) {
            return false;
        }
        return System.currentTimeMillis() - 
               timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > ttl;
    }
    
    /**
     * 检查是否为高优先级消息
     * 
     * @return 是否高优先级
     */
    public boolean isHighPriority() {
        return priority >= 8;
    }
    
    /**
     * 检查是否为请求消息
     * 
     * @return 是否为请求
     */
    public boolean isRequest() {
        return type != null && type.isRequest();
    }
    
    /**
     * 检查是否为响应消息
     * 
     * @return 是否为响应
     */
    public boolean isResponse() {
        return type != null && type.isResponse();
    }
    
    /**
     * 创建响应消息
     * 
     * @param responseType 响应类型
     * @param responsePayload 响应载荷
     * @return 响应消息
     */
    public MCPMessage createResponse(MCPMessageType responseType, Object responsePayload) {
        MCPMessage response = new MCPMessage(responseType, target, source, responsePayload);
        response.setSessionId(sessionId);
        response.setCorrelationId(id); // 使用原消息ID作为关联ID
        response.setPriority(priority);
        return response;
    }
    
    /**
     * 创建错误响应
     * 
     * @param errorMessage 错误消息
     * @param errorCode 错误代码
     * @return 错误响应
     */
    public MCPMessage createErrorResponse(String errorMessage, String errorCode) {
        MCPErrorPayload errorPayload = new MCPErrorPayload(errorCode, errorMessage);
        MCPMessage response = createResponse(MCPMessageType.ERROR, errorPayload);
        return response;
    }
    
    /**
     * 创建建造者
     * 
     * @return 建造者实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 消息建造者
     */
    public static class Builder {
        private final MCPMessage message = new MCPMessage();
        
        public Builder type(MCPMessageType type) {
            message.setType(type);
            return this;
        }
        
        public Builder source(String source) {
            message.setSource(source);
            return this;
        }
        
        public Builder target(String target) {
            message.setTarget(target);
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            message.setSessionId(sessionId);
            return this;
        }
        
        public Builder payload(Object payload) {
            message.setPayload(payload);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            message.setMetadata(metadata);
            return this;
        }
        
        public Builder priority(int priority) {
            message.setPriority(priority);
            return this;
        }
        
        public Builder ttl(long ttl) {
            message.setTtl(ttl);
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            message.setCorrelationId(correlationId);
            return this;
        }
        
        public MCPMessage build() {
            return message;
        }
    }
    
    @Override
    public String toString() {
        return String.format("MCPMessage{id='%s', type=%s, source='%s', target='%s', priority=%d}", 
                           id, type, source, target, priority);
    }
}
