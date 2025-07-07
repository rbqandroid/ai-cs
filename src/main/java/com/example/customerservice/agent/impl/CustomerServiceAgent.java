package com.example.customerservice.agent.impl;

import com.example.customerservice.agent.core.*;
import com.example.customerservice.service.ChatService;
import com.example.customerservice.mcp.MCPService;
import com.example.customerservice.mcp.MCPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 客服Agent实现
 * 处理客户咨询和服务请求
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@Component
public class CustomerServiceAgent extends AbstractAgent {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private MCPService mcpService;
    
    private AgentCapabilities capabilities;
    
    public CustomerServiceAgent() {
        super("customer-service-agent", "客服Agent", "处理客户咨询和服务请求", AgentType.CUSTOMER_SERVICE);
        initializeCapabilities();
    }
    
    private void initializeCapabilities() {
        this.capabilities = AgentCapabilities.builder()
                .supportedTaskTypes(Set.of(
                    "customer_inquiry",
                    "service_request", 
                    "complaint_handling",
                    "product_consultation",
                    "order_inquiry",
                    "general_chat"
                ))
                .maxConcurrentTasks(10)
                .maxTaskDurationMs(300000) // 5分钟
                .supportsStreaming(true)
                .supportsCallback(true)
                .version("1.0.0")
                .description("智能客服Agent，提供7x24小时客户服务")
                .build();
    }
    
    @Override
    protected AgentResult doExecute(AgentTask task) {
        try {
            String taskType = task.getType();
            String message = task.getParameter("message", "");
            String sessionId = task.getSessionId();
            String userId = task.getParameter("userId", "anonymous");
            
            logger.info("Processing customer service task: {} for user: {}", taskType, userId);
            
            // 获取或创建MCP上下文
            MCPContext context = mcpService.getOrCreateContext(sessionId, userId);
            
            // 根据任务类型处理
            String response = switch (taskType) {
                case "customer_inquiry" -> handleCustomerInquiry(message, context);
                case "service_request" -> handleServiceRequest(message, context);
                case "complaint_handling" -> handleComplaint(message, context);
                case "product_consultation" -> handleProductConsultation(message, context);
                case "order_inquiry" -> handleOrderInquiry(message, context);
                case "general_chat" -> handleGeneralChat(message, context);
                default -> handleGeneralChat(message, context);
            };
            
            // 更新上下文
            context.addConversationMessage("user", message);
            context.addConversationMessage("assistant", response);
            mcpService.updateContext(context);
            
            // 构建结果
            Map<String, Object> resultData = Map.of(
                "response", response,
                "sessionId", sessionId,
                "userId", userId,
                "taskType", taskType,
                "timestamp", System.currentTimeMillis()
            );
            
            return AgentResult.success(task.getId(), getId(), resultData, "Customer service task completed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing customer service task: {}", task.getId(), e);
            return AgentResult.failure(task.getId(), getId(), "Failed to process customer service task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理客户咨询
     */
    private String handleCustomerInquiry(String message, MCPContext context) {
        // 设置当前意图
        context.setCurrentIntent("customer_inquiry");

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        return response.isSuccess() ? response.getMessage() : "处理请求时发生错误";
    }
    
    /**
     * 处理服务请求
     */
    private String handleServiceRequest(String message, MCPContext context) {
        context.setCurrentIntent("service_request");

        // 分析服务请求类型
        String serviceType = analyzeServiceType(message);
        context.setEntity("serviceType", serviceType);

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        return response.isSuccess() ? response.getMessage() : "处理服务请求时发生错误";
    }
    
    /**
     * 处理投诉
     */
    private String handleComplaint(String message, MCPContext context) {
        context.setCurrentIntent("complaint_handling");
        context.setEntity("priority", "high"); // 投诉设为高优先级

        // 记录投诉信息
        context.setVariable("complaintReceived", true);
        context.setVariable("complaintTime", System.currentTimeMillis());

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        String aiResponse = response.isSuccess() ? response.getMessage() : "处理投诉时发生错误";

        return "我非常理解您的困扰，我会认真处理您的投诉。" + aiResponse;
    }
    
    /**
     * 处理产品咨询
     */
    private String handleProductConsultation(String message, MCPContext context) {
        context.setCurrentIntent("product_consultation");

        // 提取产品相关实体
        String product = extractProductEntity(message);
        if (product != null) {
            context.setEntity("product", product);
        }

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        return response.isSuccess() ? response.getMessage() : "处理产品咨询时发生错误";
    }

    /**
     * 处理订单查询
     */
    private String handleOrderInquiry(String message, MCPContext context) {
        context.setCurrentIntent("order_inquiry");

        // 提取订单号
        String orderNumber = extractOrderNumber(message);
        if (orderNumber != null) {
            context.setEntity("orderNumber", orderNumber);
        }

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        return response.isSuccess() ? response.getMessage() : "处理订单查询时发生错误";
    }

    /**
     * 处理一般聊天
     */
    private String handleGeneralChat(String message, MCPContext context) {
        context.setCurrentIntent("general_chat");

        // 创建聊天请求对象
        com.example.customerservice.dto.ChatRequest request = new com.example.customerservice.dto.ChatRequest();
        request.setMessage(message);
        request.setSessionId(context.getSessionId());
        request.setUserId(context.getUserId());

        // 调用聊天服务处理
        com.example.customerservice.dto.ChatResponse response = chatService.processMessage(request);
        return response.isSuccess() ? response.getMessage() : "处理聊天时发生错误";
    }
    
    /**
     * 分析服务请求类型
     */
    private String analyzeServiceType(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("退款") || lowerMessage.contains("refund")) {
            return "refund";
        } else if (lowerMessage.contains("换货") || lowerMessage.contains("exchange")) {
            return "exchange";
        } else if (lowerMessage.contains("维修") || lowerMessage.contains("repair")) {
            return "repair";
        } else if (lowerMessage.contains("取消") || lowerMessage.contains("cancel")) {
            return "cancellation";
        } else {
            return "general_service";
        }
    }
    
    /**
     * 提取产品实体
     */
    private String extractProductEntity(String message) {
        // 简单的产品名称提取逻辑
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("手机") || lowerMessage.contains("phone")) {
            return "手机";
        } else if (lowerMessage.contains("电脑") || lowerMessage.contains("computer")) {
            return "电脑";
        } else if (lowerMessage.contains("平板") || lowerMessage.contains("tablet")) {
            return "平板";
        }
        
        return null;
    }
    
    /**
     * 提取订单号
     */
    private String extractOrderNumber(String message) {
        // 简单的订单号提取逻辑（假设订单号为数字）
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{8,}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        return capabilities;
    }
    
    @Override
    protected void doInitialize(Map<String, Object> config) {
        super.doInitialize(config);
        
        // 初始化特定配置
        if (config != null) {
            // 可以从配置中读取特定参数
            int maxConcurrentTasks = (Integer) config.getOrDefault("maxConcurrentTasks", 10);
            long maxTaskDuration = (Long) config.getOrDefault("maxTaskDurationMs", 300000L);
            
            // 更新能力配置
            this.capabilities = AgentCapabilities.builder()
                    .supportedTaskTypes(capabilities.getSupportedTaskTypes())
                    .maxConcurrentTasks(maxConcurrentTasks)
                    .maxTaskDurationMs(maxTaskDuration)
                    .supportsStreaming(capabilities.isSupportsStreaming())
                    .supportsCallback(capabilities.isSupportsCallback())
                    .version(capabilities.getVersion())
                    .description(capabilities.getDescription())
                    .build();
        }
        
        logger.info("Customer Service Agent initialized with capabilities: {}", capabilities);
    }
    
    /**
     * 检查Agent健康状态
     */
    @Override
    public boolean isHealthy() {
        return super.isHealthy() && 
               chatService != null && 
               mcpService != null;
    }
}
