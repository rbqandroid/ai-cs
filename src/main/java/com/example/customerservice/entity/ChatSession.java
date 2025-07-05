package com.example.customerservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话实体
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SessionStatus status;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    // 构造函数
    public ChatSession() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
    }
    
    public ChatSession(String sessionId, String userId) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    // 辅助方法
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // 会话状态枚举
    public enum SessionStatus {
        ACTIVE,    // 活跃
        INACTIVE,  // 非活跃
        CLOSED     // 已关闭
    }
}
