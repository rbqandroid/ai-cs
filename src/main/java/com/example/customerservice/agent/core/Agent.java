package com.example.customerservice.agent.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent接口定义
 * 所有智能Agent的基础接口
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public interface Agent {
    
    /**
     * 获取Agent唯一标识
     * 
     * @return Agent ID
     */
    String getId();
    
    /**
     * 获取Agent名称
     * 
     * @return Agent名称
     */
    String getName();
    
    /**
     * 获取Agent描述
     * 
     * @return Agent描述
     */
    String getDescription();
    
    /**
     * 获取Agent类型
     * 
     * @return Agent类型
     */
    AgentType getType();
    
    /**
     * 获取Agent当前状态
     * 
     * @return Agent状态
     */
    AgentStatus getStatus();
    
    /**
     * 执行任务
     * 
     * @param task 要执行的任务
     * @return 任务执行结果的Future
     */
    CompletableFuture<AgentResult> execute(AgentTask task);
    
    /**
     * 初始化Agent
     * 
     * @param config 配置参数
     */
    void initialize(Map<String, Object> config);
    
    /**
     * 启动Agent
     */
    void start();
    
    /**
     * 停止Agent
     */
    void stop();
    
    /**
     * 暂停Agent
     */
    void pause();
    
    /**
     * 恢复Agent
     */
    void resume();
    
    /**
     * 检查Agent是否可以处理指定类型的任务
     * 
     * @param taskType 任务类型
     * @return 是否可以处理
     */
    boolean canHandle(String taskType);
    
    /**
     * 获取Agent能力描述
     * 
     * @return 能力列表
     */
    AgentCapabilities getCapabilities();
    
    /**
     * 获取Agent配置
     * 
     * @return 配置信息
     */
    Map<String, Object> getConfiguration();
    
    /**
     * 更新Agent配置
     * 
     * @param config 新配置
     */
    void updateConfiguration(Map<String, Object> config);
    
    /**
     * 获取Agent统计信息
     * 
     * @return 统计信息
     */
    AgentStatistics getStatistics();
    
    /**
     * 重置Agent状态
     */
    void reset();
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    boolean isHealthy();
}
