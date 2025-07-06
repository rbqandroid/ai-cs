package com.example.customerservice.agent.core;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent统计信息
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
public class AgentStatistics {
    
    private final AtomicLong totalTasks = new AtomicLong(0);
    private final AtomicLong successfulTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong currentActiveTasks = new AtomicLong(0);
    private final LocalDateTime startTime;
    private volatile LocalDateTime lastTaskTime;
    private volatile long averageExecutionTimeMs = 0;
    private volatile double successRate = 0.0;
    private volatile double tasksPerMinute = 0.0;
    
    public AgentStatistics() {
        this.startTime = LocalDateTime.now();
        this.lastTaskTime = startTime;
    }
    
    /**
     * 记录任务开始
     */
    public void recordTaskStart() {
        currentActiveTasks.incrementAndGet();
        totalTasks.incrementAndGet();
        lastTaskTime = LocalDateTime.now();
    }
    
    /**
     * 记录任务完成
     * 
     * @param success 是否成功
     * @param executionTimeMs 执行时间（毫秒）
     */
    public void recordTaskCompletion(boolean success, long executionTimeMs) {
        currentActiveTasks.decrementAndGet();
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        
        if (success) {
            successfulTasks.incrementAndGet();
        } else {
            failedTasks.incrementAndGet();
        }
        
        updateDerivedMetrics();
    }
    
    /**
     * 更新派生指标
     */
    private void updateDerivedMetrics() {
        long total = totalTasks.get();
        if (total > 0) {
            successRate = (double) successfulTasks.get() / total * 100.0;
            averageExecutionTimeMs = totalExecutionTimeMs.get() / total;
            
            // 计算每分钟任务数
            long uptimeMinutes = java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
            if (uptimeMinutes > 0) {
                tasksPerMinute = (double) total / uptimeMinutes;
            }
        }
    }
    
    // Getters
    public long getTotalTasks() { return totalTasks.get(); }
    public long getSuccessfulTasks() { return successfulTasks.get(); }
    public long getFailedTasks() { return failedTasks.get(); }
    public long getTotalExecutionTimeMs() { return totalExecutionTimeMs.get(); }
    public long getCurrentActiveTasks() { return currentActiveTasks.get(); }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getLastTaskTime() { return lastTaskTime; }
    public long getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
    public double getSuccessRate() { return successRate; }
    public double getTasksPerMinute() { return tasksPerMinute; }
    
    /**
     * 获取运行时间（分钟）
     * 
     * @return 运行时间
     */
    public long getUptimeMinutes() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * 获取运行时间（小时）
     * 
     * @return 运行时间
     */
    public long getUptimeHours() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toHours();
    }
    
    /**
     * 检查是否有活跃任务
     * 
     * @return 是否有活跃任务
     */
    public boolean hasActiveTasks() {
        return currentActiveTasks.get() > 0;
    }
    
    /**
     * 检查是否空闲
     * 
     * @return 是否空闲
     */
    public boolean isIdle() {
        return currentActiveTasks.get() == 0;
    }
    
    /**
     * 重置统计信息
     */
    public void reset() {
        totalTasks.set(0);
        successfulTasks.set(0);
        failedTasks.set(0);
        totalExecutionTimeMs.set(0);
        currentActiveTasks.set(0);
        averageExecutionTimeMs = 0;
        successRate = 0.0;
        tasksPerMinute = 0.0;
        lastTaskTime = LocalDateTime.now();
    }
    
    /**
     * 获取性能摘要
     * 
     * @return 性能摘要字符串
     */
    public String getPerformanceSummary() {
        return String.format(
            "Tasks: %d (Success: %d, Failed: %d), Success Rate: %.2f%%, " +
            "Avg Execution: %dms, Tasks/min: %.2f, Active: %d, Uptime: %dh",
            getTotalTasks(), getSuccessfulTasks(), getFailedTasks(),
            getSuccessRate(), getAverageExecutionTimeMs(), getTasksPerMinute(),
            getCurrentActiveTasks(), getUptimeHours()
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "AgentStatistics{total=%d, success=%d, failed=%d, active=%d, successRate=%.2f%%, avgTime=%dms}",
            getTotalTasks(), getSuccessfulTasks(), getFailedTasks(),
            getCurrentActiveTasks(), getSuccessRate(), getAverageExecutionTimeMs()
        );
    }
}
