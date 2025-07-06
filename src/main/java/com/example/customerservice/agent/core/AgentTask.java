package com.example.customerservice.agent.core;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Agent任务定义
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class AgentTask {
    
    private final String id;
    private final String type;
    private final String name;
    private final String description;
    private final Map<String, Object> parameters;
    private final Map<String, Object> context;
    private final int priority;
    private final LocalDateTime createdAt;
    private final LocalDateTime deadline;
    private final String requester;
    private final String sessionId;
    
    private AgentTask(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.type = builder.type;
        this.name = builder.name;
        this.description = builder.description;
        this.parameters = builder.parameters;
        this.context = builder.context;
        this.priority = builder.priority;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.deadline = builder.deadline;
        this.requester = builder.requester;
        this.sessionId = builder.sessionId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return parameters; }
    public Map<String, Object> getContext() { return context; }
    public int getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getRequester() { return requester; }
    public String getSessionId() { return sessionId; }
    
    /**
     * 获取参数值
     * 
     * @param key 参数键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        return parameters != null ? (T) parameters.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 获取上下文值
     * 
     * @param key 上下文键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 上下文值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        return context != null ? (T) context.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 检查是否已过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return deadline != null && LocalDateTime.now().isAfter(deadline);
    }
    
    /**
     * 检查是否为高优先级任务
     * 
     * @return 是否高优先级
     */
    public boolean isHighPriority() {
        return priority >= 8;
    }
    
    /**
     * 创建任务构建器
     * 
     * @param type 任务类型
     * @return 构建器
     */
    public static Builder builder(String type) {
        return new Builder(type);
    }
    
    /**
     * 任务构建器
     */
    public static class Builder {
        private String id;
        private final String type;
        private String name;
        private String description;
        private Map<String, Object> parameters;
        private Map<String, Object> context;
        private int priority = 5; // 默认优先级
        private LocalDateTime createdAt;
        private LocalDateTime deadline;
        private String requester;
        private String sessionId;
        
        private Builder(String type) {
            this.type = type;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = Math.max(1, Math.min(10, priority)); // 限制在1-10范围内
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder deadline(LocalDateTime deadline) {
            this.deadline = deadline;
            return this;
        }
        
        public Builder requester(String requester) {
            this.requester = requester;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public AgentTask build() {
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("Task type cannot be null or empty");
            }
            return new AgentTask(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AgentTask{id='%s', type='%s', name='%s', priority=%d}", 
                           id, type, name, priority);
    }
}
