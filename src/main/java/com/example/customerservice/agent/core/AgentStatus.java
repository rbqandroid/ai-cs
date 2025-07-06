package com.example.customerservice.agent.core;

/**
 * Agent状态枚举
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public enum AgentStatus {
    
    /**
     * 初始化中
     */
    INITIALIZING("initializing", "初始化中", "Agent正在进行初始化"),
    
    /**
     * 就绪状态
     */
    READY("ready", "就绪", "Agent已就绪，可以接收任务"),
    
    /**
     * 运行中
     */
    RUNNING("running", "运行中", "Agent正在执行任务"),
    
    /**
     * 忙碌状态
     */
    BUSY("busy", "忙碌", "Agent正在处理多个任务，负载较高"),
    
    /**
     * 暂停状态
     */
    PAUSED("paused", "暂停", "Agent已暂停，不接收新任务"),
    
    /**
     * 停止状态
     */
    STOPPED("stopped", "停止", "Agent已停止运行"),
    
    /**
     * 错误状态
     */
    ERROR("error", "错误", "Agent遇到错误，需要处理"),
    
    /**
     * 维护状态
     */
    MAINTENANCE("maintenance", "维护中", "Agent正在进行维护"),
    
    /**
     * 不可用状态
     */
    UNAVAILABLE("unavailable", "不可用", "Agent当前不可用");
    
    private final String code;
    private final String name;
    private final String description;
    
    AgentStatus(String code, String name, String description) {
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
     * 根据代码获取Agent状态
     * 
     * @param code 状态代码
     * @return Agent状态
     */
    public static AgentStatus fromCode(String code) {
        for (AgentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown agent status code: " + code);
    }
    
    /**
     * 检查是否为活跃状态
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return this == READY || this == RUNNING || this == BUSY;
    }
    
    /**
     * 检查是否可以接收任务
     * 
     * @return 是否可以接收任务
     */
    public boolean canAcceptTasks() {
        return this == READY || this == RUNNING;
    }
    
    /**
     * 检查是否为错误状态
     * 
     * @return 是否为错误状态
     */
    public boolean isError() {
        return this == ERROR;
    }
    
    /**
     * 检查是否已停止
     * 
     * @return 是否已停止
     */
    public boolean isStopped() {
        return this == STOPPED || this == UNAVAILABLE;
    }
}
