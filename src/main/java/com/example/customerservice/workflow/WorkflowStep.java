package com.example.customerservice.workflow;

import java.util.Map;

/**
 * 工作流步骤
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class WorkflowStep {
    
    private String id;
    private String name;
    private String description;
    private WorkflowStepType type;
    private int order;
    private Map<String, Object> parameters;
    private Map<String, Object> conditions;
    private String nextStepId;
    private String errorStepId;
    private long timeoutMs;
    private int retryCount;
    private boolean required;
    private boolean enabled;
    
    public WorkflowStep() {
        this.enabled = true;
        this.required = true;
        this.retryCount = 0;
        this.timeoutMs = 60000; // 1分钟默认超时
    }
    
    public WorkflowStep(String id, String name, WorkflowStepType type) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public WorkflowStepType getType() { return type; }
    public void setType(WorkflowStepType type) { this.type = type; }
    
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
    
    public String getNextStepId() { return nextStepId; }
    public void setNextStepId(String nextStepId) { this.nextStepId = nextStepId; }
    
    public String getErrorStepId() { return errorStepId; }
    public void setErrorStepId(String errorStepId) { this.errorStepId = errorStepId; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = Math.max(0, retryCount); }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    /**
     * 获取参数值
     * 
     * @param key 参数键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        return parameters != null ? (T) parameters.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置参数值
     * 
     * @param key 参数键
     * @param value 参数值
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new java.util.HashMap<>();
        }
        parameters.put(key, value);
    }
    
    /**
     * 获取条件值
     * 
     * @param key 条件键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 条件值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCondition(String key, T defaultValue) {
        return conditions != null ? (T) conditions.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置条件值
     * 
     * @param key 条件键
     * @param value 条件值
     */
    public void setCondition(String key, Object value) {
        if (conditions == null) {
            conditions = new java.util.HashMap<>();
        }
        conditions.put(key, value);
    }
    
    /**
     * 检查步骤是否可以执行
     * 
     * @return 是否可以执行
     */
    public boolean canExecute() {
        return enabled && type != null;
    }
    
    /**
     * 检查是否为开始步骤
     * 
     * @return 是否为开始步骤
     */
    public boolean isStartStep() {
        return type == WorkflowStepType.START;
    }
    
    /**
     * 检查是否为结束步骤
     * 
     * @return 是否为结束步骤
     */
    public boolean isEndStep() {
        return type == WorkflowStepType.END;
    }
    
    /**
     * 检查是否为条件步骤
     * 
     * @return 是否为条件步骤
     */
    public boolean isConditionalStep() {
        return type == WorkflowStepType.CONDITION;
    }
    
    /**
     * 检查是否为并行步骤
     * 
     * @return 是否为并行步骤
     */
    public boolean isParallelStep() {
        return type == WorkflowStepType.PARALLEL;
    }
    
    /**
     * 检查是否为Agent步骤
     * 
     * @return 是否为Agent步骤
     */
    public boolean isAgentStep() {
        return type == WorkflowStepType.AGENT_TASK;
    }
    
    /**
     * 检查是否为人工步骤
     * 
     * @return 是否为人工步骤
     */
    public boolean isHumanStep() {
        return type == WorkflowStepType.HUMAN_TASK;
    }
    
    /**
     * 检查是否为脚本步骤
     * 
     * @return 是否为脚本步骤
     */
    public boolean isScriptStep() {
        return type == WorkflowStepType.SCRIPT;
    }
    
    /**
     * 检查是否为服务调用步骤
     * 
     * @return 是否为服务调用步骤
     */
    public boolean isServiceStep() {
        return type == WorkflowStepType.SERVICE_CALL;
    }
    
    /**
     * 检查是否为等待步骤
     * 
     * @return 是否为等待步骤
     */
    public boolean isWaitStep() {
        return type == WorkflowStepType.WAIT;
    }
    
    /**
     * 验证步骤配置
     * 
     * @return 验证结果
     */
    public boolean validate() {
        if (id == null || name == null || type == null) {
            return false;
        }
        
        // 根据步骤类型验证必需参数
        switch (type) {
            case AGENT_TASK:
                return getParameter("agentId", null) != null;
            case SERVICE_CALL:
                return getParameter("serviceUrl", null) != null;
            case SCRIPT:
                return getParameter("script", null) != null;
            case WAIT:
                return getParameter("waitTimeMs", 0L) > 0;
            case CONDITION:
                return conditions != null && !conditions.isEmpty();
            default:
                return true;
        }
    }
    
    /**
     * 创建步骤副本
     * 
     * @return 步骤副本
     */
    public WorkflowStep copy() {
        WorkflowStep copy = new WorkflowStep();
        copy.id = this.id + "_copy";
        copy.name = this.name + " (Copy)";
        copy.description = this.description;
        copy.type = this.type;
        copy.order = this.order;
        copy.parameters = this.parameters != null ? new java.util.HashMap<>(this.parameters) : null;
        copy.conditions = this.conditions != null ? new java.util.HashMap<>(this.conditions) : null;
        copy.nextStepId = this.nextStepId;
        copy.errorStepId = this.errorStepId;
        copy.timeoutMs = this.timeoutMs;
        copy.retryCount = this.retryCount;
        copy.required = this.required;
        copy.enabled = this.enabled;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowStep{id='%s', name='%s', type=%s, order=%d}", 
                           id, name, type, order);
    }
}
