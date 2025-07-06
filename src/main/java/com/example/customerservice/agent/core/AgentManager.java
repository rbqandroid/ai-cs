package com.example.customerservice.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent管理器
 * 负责Agent的注册、发现、调度和生命周期管理
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@Component
public class AgentManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);
    
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> taskTypeToAgents = new ConcurrentHashMap<>();
    
    /**
     * 注册Agent
     * 
     * @param agent 要注册的Agent
     */
    public void registerAgent(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }
        
        String agentId = agent.getId();
        if (agents.containsKey(agentId)) {
            logger.warn("Agent {} is already registered, replacing it", agentId);
        }
        
        agents.put(agentId, agent);
        
        // 更新任务类型映射
        AgentCapabilities capabilities = agent.getCapabilities();
        if (capabilities != null && capabilities.getSupportedTaskTypes() != null) {
            for (String taskType : capabilities.getSupportedTaskTypes()) {
                taskTypeToAgents.computeIfAbsent(taskType, k -> ConcurrentHashMap.newKeySet()).add(agentId);
            }
        }
        
        logger.info("Agent {} registered successfully", agentId);
    }
    
    /**
     * 注销Agent
     * 
     * @param agentId Agent ID
     */
    public void unregisterAgent(String agentId) {
        Agent agent = agents.remove(agentId);
        if (agent != null) {
            // 停止Agent
            try {
                agent.stop();
            } catch (Exception e) {
                logger.error("Error stopping agent {}", agentId, e);
            }
            
            // 从任务类型映射中移除
            taskTypeToAgents.values().forEach(agentSet -> agentSet.remove(agentId));
            
            logger.info("Agent {} unregistered successfully", agentId);
        }
    }
    
    /**
     * 获取Agent
     * 
     * @param agentId Agent ID
     * @return Agent实例
     */
    public Optional<Agent> getAgent(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }
    
    /**
     * 获取所有Agent
     * 
     * @return Agent列表
     */
    public List<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }
    
    /**
     * 根据类型获取Agent
     * 
     * @param type Agent类型
     * @return Agent列表
     */
    public List<Agent> getAgentsByType(AgentType type) {
        return agents.values().stream()
                .filter(agent -> agent.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据状态获取Agent
     * 
     * @param status Agent状态
     * @return Agent列表
     */
    public List<Agent> getAgentsByStatus(AgentStatus status) {
        return agents.values().stream()
                .filter(agent -> agent.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * 查找可以处理指定任务类型的Agent
     * 
     * @param taskType 任务类型
     * @return 可用的Agent列表
     */
    public List<Agent> findAvailableAgents(String taskType) {
        Set<String> agentIds = taskTypeToAgents.get(taskType);
        if (agentIds == null || agentIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return agentIds.stream()
                .map(agents::get)
                .filter(Objects::nonNull)
                .filter(agent -> agent.getStatus().canAcceptTasks())
                .filter(agent -> agent.canHandle(taskType))
                .collect(Collectors.toList());
    }
    
    /**
     * 选择最佳Agent执行任务
     * 
     * @param task 任务
     * @return 最佳Agent
     */
    public Optional<Agent> selectBestAgent(AgentTask task) {
        List<Agent> availableAgents = findAvailableAgents(task.getType());
        
        if (availableAgents.isEmpty()) {
            return Optional.empty();
        }
        
        // 选择策略：优先选择负载最低的Agent
        return availableAgents.stream()
                .min(Comparator.comparingLong(agent -> agent.getStatistics().getCurrentActiveTasks()));
    }
    
    /**
     * 执行任务
     * 
     * @param task 任务
     * @return 执行结果的Future
     */
    public CompletableFuture<AgentResult> executeTask(AgentTask task) {
        Optional<Agent> agentOpt = selectBestAgent(task);
        
        if (agentOpt.isEmpty()) {
            return CompletableFuture.completedFuture(
                AgentResult.failure(task.getId(), "system", "No available agent found for task type: " + task.getType())
            );
        }
        
        Agent agent = agentOpt.get();
        logger.info("Executing task {} with agent {}", task.getId(), agent.getId());
        
        return agent.execute(task);
    }
    
    /**
     * 批量执行任务
     * 
     * @param tasks 任务列表
     * @return 执行结果的Future列表
     */
    public List<CompletableFuture<AgentResult>> executeTasks(List<AgentTask> tasks) {
        return tasks.stream()
                .map(this::executeTask)
                .collect(Collectors.toList());
    }
    
    /**
     * 启动所有Agent
     */
    public void startAllAgents() {
        agents.values().forEach(agent -> {
            try {
                agent.start();
            } catch (Exception e) {
                logger.error("Error starting agent {}", agent.getId(), e);
            }
        });
        logger.info("Started {} agents", agents.size());
    }
    
    /**
     * 停止所有Agent
     */
    public void stopAllAgents() {
        agents.values().forEach(agent -> {
            try {
                agent.stop();
            } catch (Exception e) {
                logger.error("Error stopping agent {}", agent.getId(), e);
            }
        });
        logger.info("Stopped {} agents", agents.size());
    }
    
    /**
     * 获取系统统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalAgents", agents.size());
        stats.put("agentsByStatus", getAgentCountByStatus());
        stats.put("agentsByType", getAgentCountByType());
        stats.put("totalActiveTasks", getTotalActiveTasks());
        stats.put("totalCompletedTasks", getTotalCompletedTasks());
        stats.put("systemSuccessRate", getSystemSuccessRate());
        
        return stats;
    }
    
    /**
     * 按状态统计Agent数量
     */
    private Map<String, Long> getAgentCountByStatus() {
        return agents.values().stream()
                .collect(Collectors.groupingBy(
                    agent -> agent.getStatus().getCode(),
                    Collectors.counting()
                ));
    }
    
    /**
     * 按类型统计Agent数量
     */
    private Map<String, Long> getAgentCountByType() {
        return agents.values().stream()
                .collect(Collectors.groupingBy(
                    agent -> agent.getType().getCode(),
                    Collectors.counting()
                ));
    }
    
    /**
     * 获取总活跃任务数
     */
    private long getTotalActiveTasks() {
        return agents.values().stream()
                .mapToLong(agent -> agent.getStatistics().getCurrentActiveTasks())
                .sum();
    }
    
    /**
     * 获取总完成任务数
     */
    private long getTotalCompletedTasks() {
        return agents.values().stream()
                .mapToLong(agent -> agent.getStatistics().getTotalTasks())
                .sum();
    }
    
    /**
     * 获取系统成功率
     */
    private double getSystemSuccessRate() {
        long totalTasks = agents.values().stream()
                .mapToLong(agent -> agent.getStatistics().getTotalTasks())
                .sum();
        
        if (totalTasks == 0) {
            return 0.0;
        }
        
        long successfulTasks = agents.values().stream()
                .mapToLong(agent -> agent.getStatistics().getSuccessfulTasks())
                .sum();
        
        return (double) successfulTasks / totalTasks * 100.0;
    }
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    public boolean isHealthy() {
        return agents.values().stream().allMatch(Agent::isHealthy);
    }
    
    /**
     * 获取不健康的Agent
     * 
     * @return 不健康的Agent列表
     */
    public List<Agent> getUnhealthyAgents() {
        return agents.values().stream()
                .filter(agent -> !agent.isHealthy())
                .collect(Collectors.toList());
    }
}
