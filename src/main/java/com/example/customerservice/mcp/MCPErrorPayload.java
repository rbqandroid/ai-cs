package com.example.customerservice.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP错误载荷
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPErrorPayload {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("details")
    private Map<String, Object> details;
    
    @JsonProperty("stackTrace")
    private String stackTrace;
    
    @JsonProperty("retryable")
    private boolean retryable = false;
    
    @JsonProperty("retryAfter")
    private Long retryAfter; // 重试间隔（毫秒）
    
    public MCPErrorPayload() {}
    
    public MCPErrorPayload(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public MCPErrorPayload(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
    
    public Long getRetryAfter() { return retryAfter; }
    public void setRetryAfter(Long retryAfter) { this.retryAfter = retryAfter; }
    
    /**
     * 添加详细信息
     * 
     * @param key 键
     * @param value 值
     */
    public void addDetail(String key, Object value) {
        if (details == null) {
            details = new java.util.HashMap<>();
        }
        details.put(key, value);
    }
    
    /**
     * 获取详细信息
     * 
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 详细信息值
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key, T defaultValue) {
        return details != null ? (T) details.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 创建常见错误类型的静态方法
     */
    
    public static MCPErrorPayload invalidRequest(String message) {
        return new MCPErrorPayload("INVALID_REQUEST", message);
    }
    
    public static MCPErrorPayload notFound(String resource) {
        return new MCPErrorPayload("NOT_FOUND", "Resource not found: " + resource);
    }
    
    public static MCPErrorPayload unauthorized(String message) {
        return new MCPErrorPayload("UNAUTHORIZED", message);
    }
    
    public static MCPErrorPayload forbidden(String message) {
        return new MCPErrorPayload("FORBIDDEN", message);
    }
    
    public static MCPErrorPayload timeout(String message) {
        MCPErrorPayload error = new MCPErrorPayload("TIMEOUT", message);
        error.setRetryable(true);
        error.setRetryAfter(5000L); // 5秒后重试
        return error;
    }
    
    public static MCPErrorPayload internalError(String message) {
        MCPErrorPayload error = new MCPErrorPayload("INTERNAL_ERROR", message);
        error.setRetryable(true);
        error.setRetryAfter(10000L); // 10秒后重试
        return error;
    }
    
    public static MCPErrorPayload serviceUnavailable(String message) {
        MCPErrorPayload error = new MCPErrorPayload("SERVICE_UNAVAILABLE", message);
        error.setRetryable(true);
        error.setRetryAfter(30000L); // 30秒后重试
        return error;
    }
    
    public static MCPErrorPayload rateLimited(String message, long retryAfter) {
        MCPErrorPayload error = new MCPErrorPayload("RATE_LIMITED", message);
        error.setRetryable(true);
        error.setRetryAfter(retryAfter);
        return error;
    }
    
    public static MCPErrorPayload validationError(String message, Map<String, Object> validationDetails) {
        MCPErrorPayload error = new MCPErrorPayload("VALIDATION_ERROR", message);
        error.setDetails(validationDetails);
        return error;
    }
    
    @Override
    public String toString() {
        return String.format("MCPErrorPayload{code='%s', message='%s', retryable=%s}", 
                           code, message, retryable);
    }
}
