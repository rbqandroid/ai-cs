package com.example.customerservice.agent.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AgentManager单元测试
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AgentManagerTest {
    
    private AgentManager agentManager;
    
    @Mock
    private Agent mockAgent1;
    
    @Mock
    private Agent mockAgent2;
    
    @Mock
    private AgentCapabilities mockCapabilities1;
    
    @Mock
    private AgentCapabilities mockCapabilities2;
    
    @Mock
    private AgentStatistics mockStatistics1;
    
    @Mock
    private AgentStatistics mockStatistics2;
    
    @BeforeEach
    void setUp() {
        agentManager = new AgentManager();
        
        // 设置mock agent 1
        when(mockAgent1.getId()).thenReturn("agent-1");
        when(mockAgent1.getName()).thenReturn("Test Agent 1");
        when(mockAgent1.getType()).thenReturn(AgentType.CUSTOMER_SERVICE);
        when(mockAgent1.getStatus()).thenReturn(AgentStatus.READY);
        when(mockAgent1.getCapabilities()).thenReturn(mockCapabilities1);
        when(mockAgent1.getStatistics()).thenReturn(mockStatistics1);
        when(mockAgent1.canHandle("test_task")).thenReturn(true);
        when(mockAgent1.isHealthy()).thenReturn(true);
        
        when(mockCapabilities1.getSupportedTaskTypes()).thenReturn(Set.of("test_task", "customer_inquiry"));
        when(mockStatistics1.getCurrentActiveTasks()).thenReturn(0L);
        when(mockStatistics1.getTotalTasks()).thenReturn(10L);
        when(mockStatistics1.getSuccessfulTasks()).thenReturn(8L);
        
        // 设置mock agent 2
        when(mockAgent2.getId()).thenReturn("agent-2");
        when(mockAgent2.getName()).thenReturn("Test Agent 2");
        when(mockAgent2.getType()).thenReturn(AgentType.RAG);
        when(mockAgent2.getStatus()).thenReturn(AgentStatus.READY);
        when(mockAgent2.getCapabilities()).thenReturn(mockCapabilities2);
        when(mockAgent2.getStatistics()).thenReturn(mockStatistics2);
        when(mockAgent2.canHandle("rag_search")).thenReturn(true);
        when(mockAgent2.isHealthy()).thenReturn(true);
        
        when(mockCapabilities2.getSupportedTaskTypes()).thenReturn(Set.of("rag_search", "vector_search"));
        when(mockStatistics2.getCurrentActiveTasks()).thenReturn(1L);
        when(mockStatistics2.getTotalTasks()).thenReturn(5L);
        when(mockStatistics2.getSuccessfulTasks()).thenReturn(5L);
    }
    
    @Test
    void testRegisterAgent() {
        // 注册Agent
        agentManager.registerAgent(mockAgent1);
        
        // 验证Agent已注册
        Optional<Agent> retrievedAgent = agentManager.getAgent("agent-1");
        assertTrue(retrievedAgent.isPresent());
        assertEquals(mockAgent1, retrievedAgent.get());
        
        // 验证任务类型映射已更新
        List<Agent> availableAgents = agentManager.findAvailableAgents("test_task");
        assertEquals(1, availableAgents.size());
        assertEquals(mockAgent1, availableAgents.get(0));
    }
    
    @Test
    void testRegisterAgentWithNullInput() {
        // 测试注册null Agent
        assertThrows(IllegalArgumentException.class, () -> {
            agentManager.registerAgent(null);
        });
    }
    
    @Test
    void testUnregisterAgent() {
        // 先注册Agent
        agentManager.registerAgent(mockAgent1);
        assertTrue(agentManager.getAgent("agent-1").isPresent());
        
        // 注销Agent
        agentManager.unregisterAgent("agent-1");
        
        // 验证Agent已注销
        assertFalse(agentManager.getAgent("agent-1").isPresent());
        verify(mockAgent1).stop();
        
        // 验证任务类型映射已清理
        List<Agent> availableAgents = agentManager.findAvailableAgents("test_task");
        assertTrue(availableAgents.isEmpty());
    }
    
    @Test
    void testGetAllAgents() {
        // 注册多个Agent
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 获取所有Agent
        List<Agent> allAgents = agentManager.getAllAgents();
        assertEquals(2, allAgents.size());
        assertTrue(allAgents.contains(mockAgent1));
        assertTrue(allAgents.contains(mockAgent2));
    }
    
    @Test
    void testGetAgentsByType() {
        // 注册不同类型的Agent
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 按类型获取Agent
        List<Agent> customerServiceAgents = agentManager.getAgentsByType(AgentType.CUSTOMER_SERVICE);
        assertEquals(1, customerServiceAgents.size());
        assertEquals(mockAgent1, customerServiceAgents.get(0));
        
        List<Agent> ragAgents = agentManager.getAgentsByType(AgentType.RAG);
        assertEquals(1, ragAgents.size());
        assertEquals(mockAgent2, ragAgents.get(0));
    }
    
    @Test
    void testGetAgentsByStatus() {
        // 设置不同状态
        when(mockAgent1.getStatus()).thenReturn(AgentStatus.READY);
        when(mockAgent2.getStatus()).thenReturn(AgentStatus.BUSY);
        
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 按状态获取Agent
        List<Agent> readyAgents = agentManager.getAgentsByStatus(AgentStatus.READY);
        assertEquals(1, readyAgents.size());
        assertEquals(mockAgent1, readyAgents.get(0));
        
        List<Agent> busyAgents = agentManager.getAgentsByStatus(AgentStatus.BUSY);
        assertEquals(1, busyAgents.size());
        assertEquals(mockAgent2, busyAgents.get(0));
    }
    
    @Test
    void testFindAvailableAgents() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 查找可处理特定任务的Agent
        List<Agent> testTaskAgents = agentManager.findAvailableAgents("test_task");
        assertEquals(1, testTaskAgents.size());
        assertEquals(mockAgent1, testTaskAgents.get(0));
        
        List<Agent> ragSearchAgents = agentManager.findAvailableAgents("rag_search");
        assertEquals(1, ragSearchAgents.size());
        assertEquals(mockAgent2, ragSearchAgents.get(0));
        
        // 查找不存在的任务类型
        List<Agent> unknownTaskAgents = agentManager.findAvailableAgents("unknown_task");
        assertTrue(unknownTaskAgents.isEmpty());
    }
    
    @Test
    void testSelectBestAgent() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 创建测试任务
        AgentTask task = AgentTask.builder("test_task")
                .name("Test Task")
                .build();
        
        // 选择最佳Agent（应该选择负载较低的）
        Optional<Agent> bestAgent = agentManager.selectBestAgent(task);
        assertTrue(bestAgent.isPresent());
        assertEquals(mockAgent1, bestAgent.get()); // agent1的活跃任务数为0，agent2为1
    }
    
    @Test
    void testSelectBestAgentNoAvailable() {
        agentManager.registerAgent(mockAgent1);
        
        // 创建无法处理的任务
        AgentTask task = AgentTask.builder("unknown_task")
                .name("Unknown Task")
                .build();
        
        // 应该没有可用的Agent
        Optional<Agent> bestAgent = agentManager.selectBestAgent(task);
        assertFalse(bestAgent.isPresent());
    }
    
    @Test
    void testExecuteTask() {
        agentManager.registerAgent(mockAgent1);
        
        // 创建测试任务
        AgentTask task = AgentTask.builder("test_task")
                .name("Test Task")
                .build();
        
        // 模拟Agent执行结果
        AgentResult expectedResult = AgentResult.success(task.getId(), "agent-1", "Task completed");
        when(mockAgent1.execute(task)).thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // 执行任务
        CompletableFuture<AgentResult> resultFuture = agentManager.executeTask(task);
        
        // 验证结果
        assertNotNull(resultFuture);
        AgentResult result = resultFuture.join();
        assertEquals(expectedResult, result);
        verify(mockAgent1).execute(task);
    }
    
    @Test
    void testExecuteTaskNoAvailableAgent() {
        // 创建无法处理的任务
        AgentTask task = AgentTask.builder("unknown_task")
                .name("Unknown Task")
                .build();
        
        // 执行任务
        CompletableFuture<AgentResult> resultFuture = agentManager.executeTask(task);
        
        // 验证结果
        assertNotNull(resultFuture);
        AgentResult result = resultFuture.join();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No available agent found"));
    }
    
    @Test
    void testStartAllAgents() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 启动所有Agent
        agentManager.startAllAgents();
        
        // 验证所有Agent都被启动
        verify(mockAgent1).start();
        verify(mockAgent2).start();
    }
    
    @Test
    void testStopAllAgents() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 停止所有Agent
        agentManager.stopAllAgents();
        
        // 验证所有Agent都被停止
        verify(mockAgent1).stop();
        verify(mockAgent2).stop();
    }
    
    @Test
    void testGetSystemStatistics() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 获取系统统计信息
        Map<String, Object> stats = agentManager.getSystemStatistics();
        
        // 验证统计信息
        assertNotNull(stats);
        assertEquals(2, stats.get("totalAgents"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> statusStats = (Map<String, Long>) stats.get("agentsByStatus");
        assertEquals(2L, statusStats.get("ready"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> typeStats = (Map<String, Long>) stats.get("agentsByType");
        assertEquals(1L, typeStats.get("customer_service"));
        assertEquals(1L, typeStats.get("rag"));
        
        assertEquals(1L, stats.get("totalActiveTasks")); // agent1: 0, agent2: 1
        assertEquals(15L, stats.get("totalCompletedTasks")); // agent1: 10, agent2: 5
    }
    
    @Test
    void testIsHealthy() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 所有Agent健康时
        assertTrue(agentManager.isHealthy());
        
        // 有Agent不健康时
        when(mockAgent1.isHealthy()).thenReturn(false);
        assertFalse(agentManager.isHealthy());
    }
    
    @Test
    void testGetUnhealthyAgents() {
        agentManager.registerAgent(mockAgent1);
        agentManager.registerAgent(mockAgent2);
        
        // 设置一个Agent不健康
        when(mockAgent1.isHealthy()).thenReturn(false);
        
        List<Agent> unhealthyAgents = agentManager.getUnhealthyAgents();
        assertEquals(1, unhealthyAgents.size());
        assertEquals(mockAgent1, unhealthyAgents.get(0));
    }
}
