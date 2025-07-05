package com.example.customerservice.dto;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 */
public class ChatResponse {
    
    private String sessionId;
    private String message;
    private String sender;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;
    private Integer tokensUsed;
    
    // 构造函数
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }
    
    public ChatResponse(String sessionId, String message, String sender) {
        this();
        this.sessionId = sessionId;
        this.message = message;
        this.sender = sender;
    }
    
    // 静态工厂方法
    public static ChatResponse success(String sessionId, String message, String sender) {
        return new ChatResponse(sessionId, message, sender);
    }
    
    public static ChatResponse error(String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        return response;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getTokensUsed() {
        return tokensUsed;
    }
    
    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }
    
    @Override
    public String toString() {
        return "ChatResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", message='" + message + '\'' +
                ", sender='" + sender + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", tokensUsed=" + tokensUsed +
                '}';
    }
}
