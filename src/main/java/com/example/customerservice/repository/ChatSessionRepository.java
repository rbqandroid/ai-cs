package com.example.customerservice.repository;

import com.example.customerservice.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 聊天会话数据访问接口
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    /**
     * 根据会话ID查找会话
     */
    Optional<ChatSession> findBySessionId(String sessionId);
    
    /**
     * 根据用户ID查找所有会话
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    /**
     * 根据用户ID和状态查找会话
     */
    List<ChatSession> findByUserIdAndStatus(String userId, ChatSession.SessionStatus status);
    
    /**
     * 查找指定时间之前更新的会话（用于清理过期会话）
     */
    @Query("SELECT s FROM ChatSession s WHERE s.updatedAt < :cutoffTime AND s.status = :status")
    List<ChatSession> findSessionsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                           @Param("status") ChatSession.SessionStatus status);
    
    /**
     * 统计用户的活跃会话数量
     */
    long countByUserIdAndStatus(String userId, ChatSession.SessionStatus status);
    
    /**
     * 检查会话是否存在
     */
    boolean existsBySessionId(String sessionId);
}
