package com.example.customerservice.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class Workflow {
    
    private String id;
    private String name;
    private String description;
    private String version;
    private WorkflowStatus status;
    private List<WorkflowStep> steps;
    private Map<String, Object> variables;
    private Map<String, Object> configuration;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean enabled;
    private int priority;
    private long timeoutMs;
    
    public Workflow() {
        this.status = WorkflowStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.enabled = true;
        this.priority = 5;
        this.timeoutMs = 300000; // 5分钟默认超时
    }
    
    public Workflow(String id, String name, String description) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    
    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }
    
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = Math.max(1, Math.min(10, priority)); }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    /**
     * 获取变量值
     * 
     * @param key 变量键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, T defaultValue) {
        return variables != null ? (T) variables.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置变量值
     * 
     * @param key 变量键
     * @param value 变量值
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new java.util.HashMap<>();
        }
        variables.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        return configuration != null ? (T) configuration.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 设置配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setConfigValue(String key, Object value) {
        if (configuration == null) {
            configuration = new java.util.HashMap<>();
        }
        configuration.put(key, value);
        updateTimestamp();
    }
    
    /**
     * 根据ID查找步骤
     * 
     * @param stepId 步骤ID
     * @return 工作流步骤
     */
    public WorkflowStep findStepById(String stepId) {
        if (steps == null) {
            return null;
        }
        return steps.stream()
                .filter(step -> stepId.equals(step.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取第一个步骤
     * 
     * @return 第一个步骤
     */
    public WorkflowStep getFirstStep() {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        return steps.stream()
                .filter(step -> step.getOrder() == 1)
                .findFirst()
                .orElse(steps.get(0));
    }
    
    /**
     * 获取下一个步骤
     * 
     * @param currentStep 当前步骤
     * @return 下一个步骤
     */
    public WorkflowStep getNextStep(WorkflowStep currentStep) {
        if (steps == null || currentStep == null) {
            return null;
        }
        
        int nextOrder = currentStep.getOrder() + 1;
        return steps.stream()
                .filter(step -> step.getOrder() == nextOrder)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查工作流是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        if (id == null || name == null || steps == null || steps.isEmpty()) {
            return false;
        }
        
        // 检查步骤顺序是否连续
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getOrder() != i + 1) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查是否可以执行
     * 
     * @return 是否可以执行
     */
    public boolean canExecute() {
        return enabled && 
               status == WorkflowStatus.ACTIVE && 
               isValid();
    }
    
    /**
     * 添加步骤
     * 
     * @param step 工作流步骤
     */
    public void addStep(WorkflowStep step) {
        if (steps == null) {
            steps = new java.util.ArrayList<>();
        }
        
        // 设置步骤顺序
        step.setOrder(steps.size() + 1);
        steps.add(step);
        updateTimestamp();
    }
    
    /**
     * 移除步骤
     * 
     * @param stepId 步骤ID
     * @return 是否成功移除
     */
    public boolean removeStep(String stepId) {
        if (steps == null) {
            return false;
        }
        
        boolean removed = steps.removeIf(step -> stepId.equals(step.getId()));
        if (removed) {
            // 重新排序
            for (int i = 0; i < steps.size(); i++) {
                steps.get(i).setOrder(i + 1);
            }
            updateTimestamp();
        }
        
        return removed;
    }
    
    /**
     * 更新时间戳
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 创建工作流副本
     * 
     * @return 工作流副本
     */
    public Workflow copy() {
        Workflow copy = new Workflow();
        copy.id = this.id + "_copy";
        copy.name = this.name + " (Copy)";
        copy.description = this.description;
        copy.version = this.version;
        copy.status = WorkflowStatus.DRAFT;
        copy.steps = this.steps != null ? new java.util.ArrayList<>(this.steps) : null;
        copy.variables = this.variables != null ? new java.util.HashMap<>(this.variables) : null;
        copy.configuration = this.configuration != null ? new java.util.HashMap<>(this.configuration) : null;
        copy.enabled = this.enabled;
        copy.priority = this.priority;
        copy.timeoutMs = this.timeoutMs;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Workflow{id='%s', name='%s', status=%s, steps=%d}", 
                           id, name, status, steps != null ? steps.size() : 0);
    }
}
