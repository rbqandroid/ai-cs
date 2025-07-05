package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求DTO
 */
public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    private String message;
    
    private String sessionId;
    
    private String userId;
    
    // 构造函数
    public ChatRequest() {}
    
    public ChatRequest(String message, String sessionId, String userId) {
        this.message = message;
        this.sessionId = sessionId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
