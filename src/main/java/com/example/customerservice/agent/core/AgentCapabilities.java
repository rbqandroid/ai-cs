package com.example.customerservice.agent.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent能力描述
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class AgentCapabilities {
    
    private final Set<String> supportedTaskTypes;
    private final Map<String, Object> parameters;
    private final List<String> requiredPermissions;
    private final Map<String, String> inputFormats;
    private final Map<String, String> outputFormats;
    private final int maxConcurrentTasks;
    private final long maxTaskDurationMs;
    private final boolean supportsStreaming;
    private final boolean supportsCallback;
    private final String version;
    private final String description;
    
    private AgentCapabilities(Builder builder) {
        this.supportedTaskTypes = builder.supportedTaskTypes;
        this.parameters = builder.parameters;
        this.requiredPermissions = builder.requiredPermissions;
        this.inputFormats = builder.inputFormats;
        this.outputFormats = builder.outputFormats;
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.maxTaskDurationMs = builder.maxTaskDurationMs;
        this.supportsStreaming = builder.supportsStreaming;
        this.supportsCallback = builder.supportsCallback;
        this.version = builder.version;
        this.description = builder.description;
    }
    
    // Getters
    public Set<String> getSupportedTaskTypes() { return supportedTaskTypes; }
    public Map<String, Object> getParameters() { return parameters; }
    public List<String> getRequiredPermissions() { return requiredPermissions; }
    public Map<String, String> getInputFormats() { return inputFormats; }
    public Map<String, String> getOutputFormats() { return outputFormats; }
    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public long getMaxTaskDurationMs() { return maxTaskDurationMs; }
    public boolean isSupportsStreaming() { return supportsStreaming; }
    public boolean isSupportsCallback() { return supportsCallback; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    
    /**
     * 检查是否支持指定任务类型
     * 
     * @param taskType 任务类型
     * @return 是否支持
     */
    public boolean supportsTaskType(String taskType) {
        return supportedTaskTypes != null && supportedTaskTypes.contains(taskType);
    }
    
    /**
     * 检查是否支持指定输入格式
     * 
     * @param format 格式
     * @return 是否支持
     */
    public boolean supportsInputFormat(String format) {
        return inputFormats != null && inputFormats.containsKey(format);
    }
    
    /**
     * 检查是否支持指定输出格式
     * 
     * @param format 格式
     * @return 是否支持
     */
    public boolean supportsOutputFormat(String format) {
        return outputFormats != null && outputFormats.containsKey(format);
    }
    
    /**
     * 创建能力构建器
     * 
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 能力构建器
     */
    public static class Builder {
        private Set<String> supportedTaskTypes;
        private Map<String, Object> parameters;
        private List<String> requiredPermissions;
        private Map<String, String> inputFormats;
        private Map<String, String> outputFormats;
        private int maxConcurrentTasks = 1;
        private long maxTaskDurationMs = 300000; // 5分钟默认
        private boolean supportsStreaming = false;
        private boolean supportsCallback = false;
        private String version = "1.0.0";
        private String description;
        
        public Builder supportedTaskTypes(Set<String> supportedTaskTypes) {
            this.supportedTaskTypes = supportedTaskTypes;
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder requiredPermissions(List<String> requiredPermissions) {
            this.requiredPermissions = requiredPermissions;
            return this;
        }
        
        public Builder inputFormats(Map<String, String> inputFormats) {
            this.inputFormats = inputFormats;
            return this;
        }
        
        public Builder outputFormats(Map<String, String> outputFormats) {
            this.outputFormats = outputFormats;
            return this;
        }
        
        public Builder maxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            return this;
        }
        
        public Builder maxTaskDurationMs(long maxTaskDurationMs) {
            this.maxTaskDurationMs = maxTaskDurationMs;
            return this;
        }
        
        public Builder supportsStreaming(boolean supportsStreaming) {
            this.supportsStreaming = supportsStreaming;
            return this;
        }
        
        public Builder supportsCallback(boolean supportsCallback) {
            this.supportsCallback = supportsCallback;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public AgentCapabilities build() {
            return new AgentCapabilities(this);
        }
    }
}
