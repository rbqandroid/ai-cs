package com.example.customerservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType;
    
    @Column(name = "sender")
    private String sender;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    // 构造函数
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatMessage(String content, MessageType messageType, String sender) {
        this();
        this.content = content;
        this.messageType = messageType;
        this.sender = sender;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChatSession getSession() {
        return session;
    }
    
    public void setSession(ChatSession session) {
        this.session = session;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Integer getTokensUsed() {
        return tokensUsed;
    }
    
    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }
    
    // 消息类型枚举
    public enum MessageType {
        USER,      // 用户消息
        ASSISTANT, // AI助手消息
        SYSTEM     // 系统消息
    }
}
