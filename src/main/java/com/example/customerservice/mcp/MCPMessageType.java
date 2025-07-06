package com.example.customerservice.mcp;

/**
 * MCP消息类型枚举
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public enum MCPMessageType {
    
    // 请求类型
    CONTEXT_REQUEST("context_request", "上下文请求", true, false),
    TASK_REQUEST("task_request", "任务请求", true, false),
    CAPABILITY_REQUEST("capability_request", "能力查询请求", true, false),
    STATUS_REQUEST("status_request", "状态查询请求", true, false),
    HEALTH_REQUEST("health_request", "健康检查请求", true, false),
    
    // 响应类型
    CONTEXT_RESPONSE("context_response", "上下文响应", false, true),
    TASK_RESPONSE("task_response", "任务响应", false, true),
    CAPABILITY_RESPONSE("capability_response", "能力查询响应", false, true),
    STATUS_RESPONSE("status_response", "状态查询响应", false, true),
    HEALTH_RESPONSE("health_response", "健康检查响应", false, true),
    
    // 通知类型
    CONTEXT_UPDATE("context_update", "上下文更新通知", false, false),
    STATUS_UPDATE("status_update", "状态更新通知", false, false),
    TASK_PROGRESS("task_progress", "任务进度通知", false, false),
    AGENT_REGISTERED("agent_registered", "Agent注册通知", false, false),
    AGENT_UNREGISTERED("agent_unregistered", "Agent注销通知", false, false),
    
    // 控制类型
    START_COMMAND("start_command", "启动命令", false, false),
    STOP_COMMAND("stop_command", "停止命令", false, false),
    PAUSE_COMMAND("pause_command", "暂停命令", false, false),
    RESUME_COMMAND("resume_command", "恢复命令", false, false),
    RESET_COMMAND("reset_command", "重置命令", false, false),
    
    // 数据传输类型
    CONTEXT_SHARE("context_share", "上下文共享", false, false),
    MODEL_SYNC("model_sync", "模型同步", false, false),
    KNOWLEDGE_SYNC("knowledge_sync", "知识同步", false, false),
    
    // 错误类型
    ERROR("error", "错误响应", false, true),
    TIMEOUT("timeout", "超时", false, false),
    
    // 心跳类型
    HEARTBEAT("heartbeat", "心跳", false, false),
    PING("ping", "Ping", true, false),
    PONG("pong", "Pong", false, true);
    
    private final String code;
    private final String description;
    private final boolean isRequest;
    private final boolean isResponse;
    
    MCPMessageType(String code, String description, boolean isRequest, boolean isResponse) {
        this.code = code;
        this.description = description;
        this.isRequest = isRequest;
        this.isResponse = isResponse;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isRequest() {
        return isRequest;
    }
    
    public boolean isResponse() {
        return isResponse;
    }
    
    /**
     * 检查是否为通知类型
     * 
     * @return 是否为通知
     */
    public boolean isNotification() {
        return !isRequest && !isResponse;
    }
    
    /**
     * 检查是否为控制命令
     * 
     * @return 是否为控制命令
     */
    public boolean isCommand() {
        return code.endsWith("_command");
    }
    
    /**
     * 检查是否为数据传输类型
     * 
     * @return 是否为数据传输
     */
    public boolean isDataTransfer() {
        return code.contains("_share") || code.contains("_sync");
    }
    
    /**
     * 检查是否为错误类型
     * 
     * @return 是否为错误
     */
    public boolean isError() {
        return this == ERROR || this == TIMEOUT;
    }
    
    /**
     * 检查是否为心跳类型
     * 
     * @return 是否为心跳
     */
    public boolean isHeartbeat() {
        return this == HEARTBEAT || this == PING || this == PONG;
    }
    
    /**
     * 获取对应的响应类型
     * 
     * @return 响应类型
     */
    public MCPMessageType getResponseType() {
        switch (this) {
            case CONTEXT_REQUEST:
                return CONTEXT_RESPONSE;
            case TASK_REQUEST:
                return TASK_RESPONSE;
            case CAPABILITY_REQUEST:
                return CAPABILITY_RESPONSE;
            case STATUS_REQUEST:
                return STATUS_RESPONSE;
            case HEALTH_REQUEST:
                return HEALTH_RESPONSE;
            case PING:
                return PONG;
            default:
                return null;
        }
    }
    
    /**
     * 根据代码获取消息类型
     * 
     * @param code 类型代码
     * @return 消息类型
     */
    public static MCPMessageType fromCode(String code) {
        for (MCPMessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MCP message type code: " + code);
    }
    
    /**
     * 获取所有请求类型
     * 
     * @return 请求类型数组
     */
    public static MCPMessageType[] getRequestTypes() {
        return java.util.Arrays.stream(values())
                .filter(MCPMessageType::isRequest)
                .toArray(MCPMessageType[]::new);
    }
    
    /**
     * 获取所有响应类型
     * 
     * @return 响应类型数组
     */
    public static MCPMessageType[] getResponseTypes() {
        return java.util.Arrays.stream(values())
                .filter(MCPMessageType::isResponse)
                .toArray(MCPMessageType[]::new);
    }
    
    /**
     * 获取所有通知类型
     * 
     * @return 通知类型数组
     */
    public static MCPMessageType[] getNotificationTypes() {
        return java.util.Arrays.stream(values())
                .filter(MCPMessageType::isNotification)
                .toArray(MCPMessageType[]::new);
    }
}
