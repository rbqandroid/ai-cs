package com.example.customerservice.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP上下文
 * 用于在模型间共享上下文信息
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPContext {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("conversationHistory")
    private java.util.List<ConversationMessage> conversationHistory;
    
    @JsonProperty("userProfile")
    private Map<String, Object> userProfile;
    
    @JsonProperty("preferences")
    private Map<String, Object> preferences;
    
    @JsonProperty("currentIntent")
    private String currentIntent;
    
    @JsonProperty("entities")
    private Map<String, Object> entities;
    
    @JsonProperty("variables")
    private Map<String, Object> variables;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    @JsonProperty("version")
    private int version = 1;
    
    public MCPContext() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.conversationHistory = new java.util.ArrayList<>();
        this.userProfile = new ConcurrentHashMap<>();
        this.preferences = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
        this.variables = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    public MCPContext(String sessionId, String userId) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public java.util.List<ConversationMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(java.util.List<ConversationMessage> conversationHistory) { 
        this.conversationHistory = conversationHistory; 
    }
    
    public Map<String, Object> getUserProfile() { return userProfile; }
    public void setUserProfile(Map<String, Object> userProfile) { this.userProfile = userProfile; }
    
    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }
    
    public String getCurrentIntent() { return currentIntent; }
    public void setCurrentIntent(String currentIntent) { this.currentIntent = currentIntent; }
    
    public Map<String, Object> getEntities() { return entities; }
    public void setEntities(Map<String, Object> entities) { this.entities = entities; }
    
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    /**
     * 添加对话消息
     * 
     * @param message 消息
     */
    public void addConversationMessage(ConversationMessage message) {
        if (conversationHistory == null) {
            conversationHistory = new java.util.ArrayList<>();
        }
        conversationHistory.add(message);
        updateTimestamp();
    }
    
    /**
     * 添加对话消息
     * 
     * @param role 角色
     * @param content 内容
     */
    public void addConversationMessage(String role, String content) {
        addConversationMessage(new ConversationMessage(role, content));
    }
    
    /**
     * 获取最近的对话消息
     * 
     * @param count 消息数量
     * @return 最近的消息列表
     */
    public java.util.List<ConversationMessage> getRecentMessages(int count) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        int size = conversationHistory.size();
        int fromIndex = Math.max(0, size - count);
        return conversationHistory.subList(fromIndex, size);
    }
    
    /**
     * 设置用户档案属性
     * 
     * @param key 键
     * @param value 值
     */
    public void setUserProfileAttribute(String key, Object value) {
        if (userProfile == null) {
            userProfile = new ConcurrentHashMap<>();
        }
        userProfile.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取用户档案属性
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserProfileAttribute(String key, T defaultValue) {
        return userProfile != null ? (T) userProfile.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置偏好设置
     * 
     * @param key 键
     * @param value 值
     */
    public void setPreference(String key, Object value) {
        if (preferences == null) {
            preferences = new ConcurrentHashMap<>();
        }
        preferences.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取偏好设置
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 偏好值
     */
    @SuppressWarnings("unchecked")
    public <T> T getPreference(String key, T defaultValue) {
        return preferences != null ? (T) preferences.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置实体
     * 
     * @param key 键
     * @param value 值
     */
    public void setEntity(String key, Object value) {
        if (entities == null) {
            entities = new ConcurrentHashMap<>();
        }
        entities.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取实体
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 实体值
     */
    @SuppressWarnings("unchecked")
    public <T> T getEntity(String key, T defaultValue) {
        return entities != null ? (T) entities.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置变量
     * 
     * @param key 键
     * @param value 值
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new ConcurrentHashMap<>();
        }
        variables.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取变量
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, T defaultValue) {
        return variables != null ? (T) variables.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置元数据
     * 
     * @param key 键
     * @param value 值
     */
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new ConcurrentHashMap<>();
        }
        metadata.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取元数据
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 元数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, T defaultValue) {
        return metadata != null ? (T) metadata.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 更新时间戳和版本
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }
    
    /**
     * 清空对话历史
     */
    public void clearConversationHistory() {
        if (conversationHistory != null) {
            conversationHistory.clear();
        }
        updateTimestamp();
    }
    
    /**
     * 重置上下文
     */
    public void reset() {
        clearConversationHistory();
        if (entities != null) entities.clear();
        if (variables != null) variables.clear();
        currentIntent = null;
        updateTimestamp();
    }
    
    /**
     * 创建上下文副本
     * 
     * @return 上下文副本
     */
    public MCPContext copy() {
        MCPContext copy = new MCPContext(sessionId, userId);
        copy.conversationHistory = new java.util.ArrayList<>(this.conversationHistory);
        copy.userProfile = new ConcurrentHashMap<>(this.userProfile);
        copy.preferences = new ConcurrentHashMap<>(this.preferences);
        copy.currentIntent = this.currentIntent;
        copy.entities = new ConcurrentHashMap<>(this.entities);
        copy.variables = new ConcurrentHashMap<>(this.variables);
        copy.metadata = new ConcurrentHashMap<>(this.metadata);
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        copy.version = this.version;
        return copy;
    }
    
    /**
     * 对话消息内部类
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversationMessage {
        @JsonProperty("role")
        private String role;
        
        @JsonProperty("content")
        private String content;
        
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
        
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
        
        public ConversationMessage() {
            this.timestamp = LocalDateTime.now();
        }
        
        public ConversationMessage(String role, String content) {
            this();
            this.role = role;
            this.content = content;
        }
        
        // Getters and Setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
