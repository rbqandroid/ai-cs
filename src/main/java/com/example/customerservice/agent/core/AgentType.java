package com.example.customerservice.agent.core;

/**
 * Agent类型枚举
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public enum AgentType {
    
    /**
     * 客服Agent - 处理客户咨询和服务
     */
    CUSTOMER_SERVICE("customer_service", "客服Agent", "处理客户咨询、问题解答和服务支持"),
    
    /**
     * 知识库Agent - 管理和检索知识库内容
     */
    KNOWLEDGE_BASE("knowledge_base", "知识库Agent", "管理知识库内容，提供智能检索和推荐"),
    
    /**
     * RAG Agent - 检索增强生成
     */
    RAG("rag", "RAG Agent", "基于检索增强生成技术提供精准回答"),
    
    /**
     * 工作流Agent - 执行和管理工作流
     */
    WORKFLOW("workflow", "工作流Agent", "执行复杂的业务流程和任务编排"),
    
    /**
     * 分析Agent - 数据分析和洞察
     */
    ANALYTICS("analytics", "分析Agent", "提供数据分析、统计和业务洞察"),
    
    /**
     * 监控Agent - 系统监控和告警
     */
    MONITORING("monitoring", "监控Agent", "监控系统状态，提供告警和诊断"),
    
    /**
     * 协调Agent - 多Agent协调和管理
     */
    COORDINATOR("coordinator", "协调Agent", "协调多个Agent的协作和任务分配"),
    
    /**
     * 自定义Agent - 用户自定义类型
     */
    CUSTOM("custom", "自定义Agent", "用户自定义的特殊用途Agent");
    
    private final String code;
    private final String name;
    private final String description;
    
    AgentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取Agent类型
     * 
     * @param code 类型代码
     * @return Agent类型
     */
    public static AgentType fromCode(String code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown agent type code: " + code);
    }
    
    /**
     * 检查是否为系统内置类型
     * 
     * @return 是否为内置类型
     */
    public boolean isBuiltIn() {
        return this != CUSTOM;
    }
}
