package com.example.customerservice.repository;

import com.example.customerservice.entity.ChatMessage;
import com.example.customerservice.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息数据访问接口
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 根据会话查找所有消息，按创建时间排序
     */
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    
    /**
     * 根据会话ID查找消息，支持分页
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.sessionId = :sessionId ORDER BY m.createdAt ASC")
    Page<ChatMessage> findBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
    
    /**
     * 根据会话查找最近的N条消息
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session = :session ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesBySession(@Param("session") ChatSession session, Pageable pageable);
    
    /**
     * 统计会话中的消息数量
     */
    long countBySession(ChatSession session);
    
    /**
     * 根据消息类型统计数量
     */
    long countBySessionAndMessageType(ChatSession session, ChatMessage.MessageType messageType);
    
    /**
     * 查找指定时间范围内的消息
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session = :session AND m.createdAt BETWEEN :startTime AND :endTime ORDER BY m.createdAt ASC")
    List<ChatMessage> findMessagesBySessionAndTimeRange(@Param("session") ChatSession session,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定会话的所有消息
     */
    void deleteBySession(ChatSession session);
}
