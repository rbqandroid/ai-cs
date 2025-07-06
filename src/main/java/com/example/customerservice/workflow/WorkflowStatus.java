package com.example.customerservice.workflow;

/**
 * 工作流状态枚举
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public enum WorkflowStatus {
    
    /**
     * 草稿状态
     */
    DRAFT("draft", "草稿", "工作流正在编辑中"),
    
    /**
     * 活跃状态
     */
    ACTIVE("active", "活跃", "工作流已发布并可以执行"),
    
    /**
     * 暂停状态
     */
    PAUSED("paused", "暂停", "工作流已暂停，不接受新的执行请求"),
    
    /**
     * 已停用状态
     */
    DISABLED("disabled", "已停用", "工作流已停用，无法执行"),
    
    /**
     * 已归档状态
     */
    ARCHIVED("archived", "已归档", "工作流已归档，仅供查看"),
    
    /**
     * 已删除状态
     */
    DELETED("deleted", "已删除", "工作流已标记为删除");
    
    private final String code;
    private final String name;
    private final String description;
    
    WorkflowStatus(String code, String name, String description) {
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
     * 根据代码获取工作流状态
     * 
     * @param code 状态代码
     * @return 工作流状态
     */
    public static WorkflowStatus fromCode(String code) {
        for (WorkflowStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown workflow status code: " + code);
    }
    
    /**
     * 检查是否可以执行
     * 
     * @return 是否可以执行
     */
    public boolean canExecute() {
        return this == ACTIVE;
    }
    
    /**
     * 检查是否可以编辑
     * 
     * @return 是否可以编辑
     */
    public boolean canEdit() {
        return this == DRAFT || this == PAUSED;
    }
    
    /**
     * 检查是否为终态
     * 
     * @return 是否为终态
     */
    public boolean isFinal() {
        return this == ARCHIVED || this == DELETED;
    }
    
    /**
     * 检查是否为活跃状态
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
