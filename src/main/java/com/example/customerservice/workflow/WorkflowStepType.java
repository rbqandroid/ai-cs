package com.example.customerservice.workflow;

/**
 * 工作流步骤类型枚举
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public enum WorkflowStepType {
    
    /**
     * 开始步骤
     */
    START("start", "开始", "工作流的起始步骤"),
    
    /**
     * 结束步骤
     */
    END("end", "结束", "工作流的结束步骤"),
    
    /**
     * Agent任务步骤
     */
    AGENT_TASK("agent_task", "Agent任务", "由AI Agent执行的任务步骤"),
    
    /**
     * 人工任务步骤
     */
    HUMAN_TASK("human_task", "人工任务", "需要人工干预的任务步骤"),
    
    /**
     * 服务调用步骤
     */
    SERVICE_CALL("service_call", "服务调用", "调用外部服务的步骤"),
    
    /**
     * 脚本执行步骤
     */
    SCRIPT("script", "脚本执行", "执行自定义脚本的步骤"),
    
    /**
     * 条件判断步骤
     */
    CONDITION("condition", "条件判断", "根据条件决定流程走向的步骤"),
    
    /**
     * 并行执行步骤
     */
    PARALLEL("parallel", "并行执行", "并行执行多个子步骤"),
    
    /**
     * 等待步骤
     */
    WAIT("wait", "等待", "等待指定时间或条件的步骤"),
    
    /**
     * 数据转换步骤
     */
    DATA_TRANSFORM("data_transform", "数据转换", "转换数据格式或内容的步骤"),
    
    /**
     * 通知步骤
     */
    NOTIFICATION("notification", "通知", "发送通知消息的步骤"),
    
    /**
     * 审批步骤
     */
    APPROVAL("approval", "审批", "需要审批的步骤"),
    
    /**
     * 循环步骤
     */
    LOOP("loop", "循环", "循环执行的步骤"),
    
    /**
     * 子工作流步骤
     */
    SUB_WORKFLOW("sub_workflow", "子工作流", "调用其他工作流的步骤"),
    
    /**
     * 自定义步骤
     */
    CUSTOM("custom", "自定义", "用户自定义的步骤类型");
    
    private final String code;
    private final String name;
    private final String description;
    
    WorkflowStepType(String code, String name, String description) {
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
     * 根据代码获取步骤类型
     * 
     * @param code 类型代码
     * @return 步骤类型
     */
    public static WorkflowStepType fromCode(String code) {
        for (WorkflowStepType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown workflow step type code: " + code);
    }
    
    /**
     * 检查是否为控制流步骤
     * 
     * @return 是否为控制流步骤
     */
    public boolean isControlFlow() {
        return this == START || this == END || this == CONDITION || 
               this == PARALLEL || this == LOOP;
    }
    
    /**
     * 检查是否为任务步骤
     * 
     * @return 是否为任务步骤
     */
    public boolean isTask() {
        return this == AGENT_TASK || this == HUMAN_TASK || 
               this == SERVICE_CALL || this == SCRIPT;
    }
    
    /**
     * 检查是否为交互步骤
     * 
     * @return 是否为交互步骤
     */
    public boolean isInteractive() {
        return this == HUMAN_TASK || this == APPROVAL;
    }
    
    /**
     * 检查是否为自动化步骤
     * 
     * @return 是否为自动化步骤
     */
    public boolean isAutomated() {
        return this == AGENT_TASK || this == SERVICE_CALL || 
               this == SCRIPT || this == DATA_TRANSFORM;
    }
    
    /**
     * 检查是否需要外部依赖
     * 
     * @return 是否需要外部依赖
     */
    public boolean requiresExternalDependency() {
        return this == SERVICE_CALL || this == NOTIFICATION || 
               this == SUB_WORKFLOW;
    }
    
    /**
     * 检查是否可以并行执行
     * 
     * @return 是否可以并行执行
     */
    public boolean canRunInParallel() {
        return this != HUMAN_TASK && this != APPROVAL && 
               this != START && this != END;
    }
    
    /**
     * 检查是否为阻塞步骤
     * 
     * @return 是否为阻塞步骤
     */
    public boolean isBlocking() {
        return this == HUMAN_TASK || this == APPROVAL || this == WAIT;
    }
    
    /**
     * 获取默认超时时间（毫秒）
     * 
     * @return 默认超时时间
     */
    public long getDefaultTimeoutMs() {
        switch (this) {
            case HUMAN_TASK:
            case APPROVAL:
                return 24 * 60 * 60 * 1000L; // 24小时
            case SERVICE_CALL:
                return 30 * 1000L; // 30秒
            case SCRIPT:
            case DATA_TRANSFORM:
                return 5 * 60 * 1000L; // 5分钟
            case AGENT_TASK:
                return 10 * 60 * 1000L; // 10分钟
            case WAIT:
                return 60 * 1000L; // 1分钟
            default:
                return 60 * 1000L; // 1分钟默认
        }
    }
    
    /**
     * 获取默认重试次数
     * 
     * @return 默认重试次数
     */
    public int getDefaultRetryCount() {
        switch (this) {
            case SERVICE_CALL:
            case AGENT_TASK:
                return 3;
            case SCRIPT:
            case DATA_TRANSFORM:
                return 1;
            case HUMAN_TASK:
            case APPROVAL:
            case WAIT:
                return 0; // 不重试
            default:
                return 1;
        }
    }
}
