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
 * 聊天API控制器 - RESTful接口层
 *
 * <p>这个控制器提供了智能客服系统的核心RESTful API接口，
 * 负责处理客户端的聊天请求、会话管理和系统监控等功能。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>聊天消息处理：接收用户消息并返回AI回复</li>
 *   <li>会话历史查询：获取指定会话的对话历史</li>
 *   <li>系统健康检查：提供服务状态监控接口</li>
 *   <li>管理功能：会话清理等管理操作</li>
 * </ul>
 *
 * <h3>API设计原则：</h3>
 * <ul>
 *   <li>RESTful风格：遵循REST API设计规范</li>
 *   <li>统一响应格式：所有接口返回统一的响应结构</li>
 *   <li>错误处理：完善的异常处理和错误信息返回</li>
 *   <li>参数验证：使用Bean Validation进行参数校验</li>
 *   <li>日志记录：详细的请求和响应日志</li>
 * </ul>
 *
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-04
 * @see ChatService
 * @see ChatRequest
 * @see ChatResponse
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // 允许跨域访问，生产环境应该限制具体域名
public class ChatController {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    /**
     * 聊天服务 - 核心业务逻辑处理
     */
    @Autowired
    private ChatService chatService;

    /**
     * 发送聊天消息接口
     *
     * <p>这是系统的核心接口，用于处理用户的聊天消息并返回AI生成的回复。
     * 支持新会话创建和现有会话的消息追加。</p>
     *
     * <h3>功能特性：</h3>
     * <ul>
     *   <li>自动会话管理：自动创建新会话或使用现有会话</li>
     *   <li>消息验证：验证消息内容的有效性</li>
     *   <li>AI处理：调用AI模型生成智能回复</li>
     *   <li>历史记录：自动保存对话历史</li>
     *   <li>Token统计：记录AI调用的Token使用量</li>
     * </ul>
     *
     * <h3>请求示例：</h3>
     * <pre>
     * POST /api/chat/message
     * Content-Type: application/json
     *
     * {
     *   "message": "你好，我想咨询产品信息",
     *   "userId": "user123",
     *   "sessionId": "session456"  // 可选，不提供则自动创建新会话
     * }
     * </pre>
     *
     * <h3>响应示例：</h3>
     * <pre>
     * {
     *   "success": true,
     *   "sessionId": "session456",
     *   "message": "您好！我是智能客服助手...",
     *   "sender": "assistant",
     *   "timestamp": "2024-07-04T10:30:00",
     *   "tokensUsed": 50
     * }
     * </pre>
     *
     * @param request 聊天请求对象，包含消息内容、用户ID等信息
     * @return ResponseEntity<ChatResponse> 包含AI回复的响应对象
     * @throws IllegalArgumentException 当请求参数无效时
     * @throws RuntimeException 当服务处理失败时
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.info("收到聊天请求: userId={}, sessionId={}, messageLength={}",
                   request.getUserId(), request.getSessionId(),
                   request.getMessage() != null ? request.getMessage().length() : 0);

        try {
            // 调用聊天服务处理消息
            ChatResponse response = chatService.processMessage(request);

            // 根据处理结果返回相应的HTTP状态码
            if (response.isSuccess()) {
                logger.info("聊天请求处理成功: sessionId={}, tokensUsed={}",
                           response.getSessionId(), response.getTokensUsed());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("聊天请求处理失败: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("处理聊天请求时发生错误: userId={}, error={}",
                        request.getUserId(), e.getMessage(), e);
            ChatResponse errorResponse = ChatResponse.error("服务器内部错误，请稍后重试");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取会话历史接口
     *
     * <p>根据会话ID获取完整的对话历史记录，包括用户消息和AI回复。
     * 返回的消息按时间顺序排列，便于客户端展示对话流程。</p>
     *
     * <h3>功能特性：</h3>
     * <ul>
     *   <li>完整历史：返回会话中的所有消息记录</li>
     *   <li>时间排序：消息按创建时间正序排列</li>
     *   <li>消息类型：区分用户消息、AI回复和系统消息</li>
     *   <li>元数据信息：包含发送者、时间戳等详细信息</li>
     * </ul>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>客户端页面刷新后恢复对话历史</li>
     *   <li>客服人员查看用户对话记录</li>
     *   <li>系统审计和质量分析</li>
     * </ul>
     *
     * @param sessionId 会话ID，用于标识特定的对话会话
     * @return ResponseEntity<List<ChatMessage>> 包含消息列表的响应
     * @throws IllegalArgumentException 当会话ID无效时
     * @throws RuntimeException 当数据库查询失败时
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getSessionHistory(@PathVariable String sessionId) {
        logger.info("获取会话历史: sessionId={}", sessionId);

        try {
            List<ChatMessage> history = chatService.getSessionHistory(sessionId);
            logger.info("成功获取会话历史: sessionId={}, messageCount={}",
                       sessionId, history.size());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取会话历史时发生错误: sessionId={}, error={}",
                        sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 系统健康检查接口
     *
     * <p>提供系统运行状态的快速检查，用于监控系统可用性和基本信息。
     * 这是一个轻量级的接口，不涉及复杂的业务逻辑或数据库操作。</p>
     *
     * <h3>返回信息：</h3>
     * <ul>
     *   <li>status: 服务状态（UP/DOWN）</li>
     *   <li>service: 服务名称标识</li>
     *   <li>timestamp: 当前服务器时间</li>
     * </ul>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>负载均衡器健康检查</li>
     *   <li>监控系统状态探测</li>
     *   <li>服务发现和注册</li>
     *   <li>运维巡检和故障排查</li>
     * </ul>
     *
     * @return ResponseEntity<Map<String, String>> 包含系统状态信息的响应
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("执行健康检查");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Customer Service AI",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "version", "1.0.0"
        ));
    }

    /**
     * 清理过期会话接口（管理功能）
     *
     * <p>这是一个管理接口，用于清理系统中的过期会话和相关数据。
     * 定期执行此操作可以释放存储空间，提高系统性能。</p>
     *
     * <h3>清理内容：</h3>
     * <ul>
     *   <li>过期会话：超过配置时间未活动的会话</li>
     *   <li>孤立消息：没有关联会话的消息记录</li>
     *   <li>临时数据：系统运行过程中产生的临时数据</li>
     * </ul>
     *
     * <h3>安全考虑：</h3>
     * <ul>
     *   <li>管理权限：应该限制只有管理员可以调用</li>
     *   <li>操作日志：记录详细的清理操作日志</li>
     *   <li>数据备份：重要数据清理前应该备份</li>
     * </ul>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>避免在业务高峰期执行</li>
     *   <li>可以配置为定时任务自动执行</li>
     *   <li>清理过程中可能影响系统性能</li>
     * </ul>
     *
     * @return ResponseEntity<Map<String, String>> 包含清理结果的响应
     * @throws RuntimeException 当清理操作失败时
     */
    @PostMapping("/admin/cleanup")
    public ResponseEntity<Map<String, String>> cleanupSessions() {
        logger.info("执行会话清理操作");

        try {
            long startTime = System.currentTimeMillis();
            chatService.cleanupExpiredSessions();
            long duration = System.currentTimeMillis() - startTime;

            logger.info("会话清理完成，耗时: {}ms", duration);
            return ResponseEntity.ok(Map.of(
                "message", "会话清理完成",
                "timestamp", java.time.LocalDateTime.now().toString(),
                "duration", duration + "ms"
            ));
        } catch (Exception e) {
            logger.error("清理会话时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "清理会话失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
}
