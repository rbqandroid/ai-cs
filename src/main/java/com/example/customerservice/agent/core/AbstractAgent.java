package com.example.customerservice.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent抽象基类
 * 提供Agent的基础实现
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public abstract class AbstractAgent implements Agent {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final String id;
    private final String name;
    private final String description;
    private final AgentType type;
    
    private volatile AgentStatus status = AgentStatus.INITIALIZING;
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();
    private final AgentStatistics statistics = new AgentStatistics();
    private ExecutorService executorService;
    private volatile boolean initialized = false;
    
    protected AbstractAgent(String id, String name, String description, AgentType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "Agent-" + id + "-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public AgentType getType() {
        return type;
    }
    
    @Override
    public AgentStatus getStatus() {
        return status;
    }
    
    @Override
    public CompletableFuture<AgentResult> execute(AgentTask task) {
        if (!canAcceptTask(task)) {
            return CompletableFuture.completedFuture(
                AgentResult.failure(task.getId(), id, "Agent cannot accept this task")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            statistics.recordTaskStart();
            
            try {
                setStatus(AgentStatus.RUNNING);
                logger.info("Agent {} starting task: {}", id, task.getId());
                
                // 执行具体任务
                AgentResult result = doExecute(task);
                
                // 记录统计信息
                long executionTime = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
                statistics.recordTaskCompletion(result.isSuccess(), executionTime);
                
                logger.info("Agent {} completed task: {} in {}ms", id, task.getId(), executionTime);
                return result;
                
            } catch (Exception e) {
                logger.error("Agent {} failed to execute task: {}", id, task.getId(), e);
                long executionTime = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
                statistics.recordTaskCompletion(false, executionTime);
                
                return AgentResult.failure(task.getId(), id, "Task execution failed: " + e.getMessage(), e);
            } finally {
                updateStatusAfterTask();
            }
        }, executorService);
    }
    
    /**
     * 执行具体任务的抽象方法
     * 子类需要实现此方法
     * 
     * @param task 任务
     * @return 执行结果
     */
    protected abstract AgentResult doExecute(AgentTask task);
    
    /**
     * 获取Agent能力
     * 子类需要实现此方法
     * 
     * @return Agent能力
     */
    @Override
    public abstract AgentCapabilities getCapabilities();
    
    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            logger.warn("Agent {} is already initialized", id);
            return;
        }
        
        try {
            setStatus(AgentStatus.INITIALIZING);
            
            if (config != null) {
                configuration.putAll(config);
            }
            
            // 执行具体初始化逻辑
            doInitialize(config);
            
            initialized = true;
            setStatus(AgentStatus.READY);
            logger.info("Agent {} initialized successfully", id);
            
        } catch (Exception e) {
            setStatus(AgentStatus.ERROR);
            logger.error("Failed to initialize agent {}", id, e);
            throw new RuntimeException("Agent initialization failed", e);
        }
    }
    
    /**
     * 执行具体初始化逻辑
     * 子类可以重写此方法
     * 
     * @param config 配置参数
     */
    protected void doInitialize(Map<String, Object> config) {
        // 默认实现为空，子类可以重写
    }
    
    @Override
    public void start() {
        if (status == AgentStatus.READY || status == AgentStatus.PAUSED) {
            setStatus(AgentStatus.READY);
            logger.info("Agent {} started", id);
        } else {
            logger.warn("Cannot start agent {} in status {}", id, status);
        }
    }
    
    @Override
    public void stop() {
        setStatus(AgentStatus.STOPPED);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        logger.info("Agent {} stopped", id);
    }
    
    @Override
    public void pause() {
        if (status.isActive()) {
            setStatus(AgentStatus.PAUSED);
            logger.info("Agent {} paused", id);
        }
    }
    
    @Override
    public void resume() {
        if (status == AgentStatus.PAUSED) {
            setStatus(AgentStatus.READY);
            logger.info("Agent {} resumed", id);
        }
    }
    
    @Override
    public boolean canHandle(String taskType) {
        AgentCapabilities capabilities = getCapabilities();
        return capabilities != null && capabilities.supportsTaskType(taskType);
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return new ConcurrentHashMap<>(configuration);
    }
    
    @Override
    public void updateConfiguration(Map<String, Object> config) {
        if (config != null) {
            configuration.putAll(config);
            logger.info("Agent {} configuration updated", id);
        }
    }
    
    @Override
    public AgentStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public void reset() {
        statistics.reset();
        setStatus(AgentStatus.READY);
        logger.info("Agent {} reset", id);
    }
    
    @Override
    public boolean isHealthy() {
        return status != AgentStatus.ERROR && status != AgentStatus.UNAVAILABLE;
    }
    
    /**
     * 检查是否可以接受任务
     * 
     * @param task 任务
     * @return 是否可以接受
     */
    protected boolean canAcceptTask(AgentTask task) {
        return status.canAcceptTasks() && 
               canHandle(task.getType()) && 
               !task.isExpired() &&
               statistics.getCurrentActiveTasks() < getCapabilities().getMaxConcurrentTasks();
    }
    
    /**
     * 设置Agent状态
     * 
     * @param newStatus 新状态
     */
    protected void setStatus(AgentStatus newStatus) {
        AgentStatus oldStatus = this.status;
        this.status = newStatus;
        logger.debug("Agent {} status changed from {} to {}", id, oldStatus, newStatus);
    }
    
    /**
     * 任务完成后更新状态
     */
    private void updateStatusAfterTask() {
        if (statistics.getCurrentActiveTasks() == 0 && status == AgentStatus.RUNNING) {
            setStatus(AgentStatus.READY);
        } else if (statistics.getCurrentActiveTasks() >= getCapabilities().getMaxConcurrentTasks()) {
            setStatus(AgentStatus.BUSY);
        }
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
    protected <T> T getConfigValue(String key, T defaultValue) {
        return (T) configuration.getOrDefault(key, defaultValue);
    }
}
