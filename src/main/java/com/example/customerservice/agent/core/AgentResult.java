package com.example.customerservice.agent.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent执行结果
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class AgentResult {
    
    private final String taskId;
    private final String agentId;
    private final boolean success;
    private final Object data;
    private final String message;
    private final String errorCode;
    private final Throwable exception;
    private final Map<String, Object> metadata;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long executionTimeMs;
    
    private AgentResult(Builder builder) {
        this.taskId = builder.taskId;
        this.agentId = builder.agentId;
        this.success = builder.success;
        this.data = builder.data;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
        this.exception = builder.exception;
        this.metadata = builder.metadata;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime != null ? builder.endTime : LocalDateTime.now();
        this.executionTimeMs = builder.executionTimeMs > 0 ? builder.executionTimeMs : 
            (startTime != null ? java.time.Duration.between(startTime, this.endTime).toMillis() : 0);
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getAgentId() { return agentId; }
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public Throwable getException() { return exception; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    
    /**
     * 获取数据并转换为指定类型
     * 
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 转换后的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(Class<T> clazz) {
        if (data == null) {
            return null;
        }
        if (clazz.isInstance(data)) {
            return (T) data;
        }
        throw new ClassCastException("Cannot cast data to " + clazz.getName());
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
     * 检查是否为失败结果
     * 
     * @return 是否失败
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 检查是否有异常
     * 
     * @return 是否有异常
     */
    public boolean hasException() {
        return exception != null;
    }
    
    /**
     * 创建成功结果
     * 
     * @param taskId 任务ID
     * @param agentId Agent ID
     * @param data 结果数据
     * @return 成功结果
     */
    public static AgentResult success(String taskId, String agentId, Object data) {
        return builder(taskId, agentId)
                .success(true)
                .data(data)
                .build();
    }
    
    /**
     * 创建成功结果（带消息）
     * 
     * @param taskId 任务ID
     * @param agentId Agent ID
     * @param data 结果数据
     * @param message 消息
     * @return 成功结果
     */
    public static AgentResult success(String taskId, String agentId, Object data, String message) {
        return builder(taskId, agentId)
                .success(true)
                .data(data)
                .message(message)
                .build();
    }
    
    /**
     * 创建失败结果
     * 
     * @param taskId 任务ID
     * @param agentId Agent ID
     * @param message 错误消息
     * @return 失败结果
     */
    public static AgentResult failure(String taskId, String agentId, String message) {
        return builder(taskId, agentId)
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 创建失败结果（带异常）
     * 
     * @param taskId 任务ID
     * @param agentId Agent ID
     * @param message 错误消息
     * @param exception 异常
     * @return 失败结果
     */
    public static AgentResult failure(String taskId, String agentId, String message, Throwable exception) {
        return builder(taskId, agentId)
                .success(false)
                .message(message)
                .exception(exception)
                .build();
    }
    
    /**
     * 创建结果构建器
     * 
     * @param taskId 任务ID
     * @param agentId Agent ID
     * @return 构建器
     */
    public static Builder builder(String taskId, String agentId) {
        return new Builder(taskId, agentId);
    }
    
    /**
     * 结果构建器
     */
    public static class Builder {
        private final String taskId;
        private final String agentId;
        private boolean success;
        private Object data;
        private String message;
        private String errorCode;
        private Throwable exception;
        private Map<String, Object> metadata;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long executionTimeMs;
        
        private Builder(String taskId, String agentId) {
            this.taskId = taskId;
            this.agentId = agentId;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public AgentResult build() {
            return new AgentResult(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AgentResult{taskId='%s', agentId='%s', success=%s, executionTimeMs=%d}", 
                           taskId, agentId, success, executionTimeMs);
    }
}
