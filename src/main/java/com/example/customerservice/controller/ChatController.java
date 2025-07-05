package com.example.customerservice.controller;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.entity.ChatMessage;
import com.example.customerservice.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天API控制器
 * 提供RESTful接口用于客服系统交互
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // 允许跨域访问，生产环境应该限制具体域名
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    /**
     * 发送聊天消息
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.info("收到聊天请求: {}", request);
        
        try {
            ChatResponse response = chatService.processMessage(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("处理聊天请求时发生错误", e);
            ChatResponse errorResponse = ChatResponse.error("服务器内部错误");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getSessionHistory(@PathVariable String sessionId) {
        logger.info("获取会话历史: {}", sessionId);
        
        try {
            List<ChatMessage> history = chatService.getSessionHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取会话历史时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Customer Service AI",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
    
    /**
     * 清理过期会话（管理接口）
     */
    @PostMapping("/admin/cleanup")
    public ResponseEntity<Map<String, String>> cleanupSessions() {
        logger.info("执行会话清理");
        
        try {
            chatService.cleanupExpiredSessions();
            return ResponseEntity.ok(Map.of(
                "message", "会话清理完成",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            logger.error("清理会话时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "清理会话失败: " + e.getMessage()
            ));
        }
    }
}
