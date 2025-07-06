package com.example.customerservice.mcp;

/**
 * MCP消息处理器接口
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public interface MCPMessageHandler {
    
    /**
     * 处理MCP消息
     * 
     * @param message 输入消息
     * @param mcpService MCP服务实例
     * @return 响应消息
     */
    MCPMessage handle(MCPMessage message, MCPService mcpService);
    
    /**
     * 获取处理器支持的消息类型
     * 
     * @return 消息类型
     */
    MCPMessageType getSupportedMessageType();
    
    /**
     * 检查是否可以处理指定消息
     * 
     * @param message 消息
     * @return 是否可以处理
     */
    default boolean canHandle(MCPMessage message) {
        return message != null && 
               message.getType() == getSupportedMessageType();
    }
}
