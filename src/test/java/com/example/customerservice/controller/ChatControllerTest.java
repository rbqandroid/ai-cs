package com.example.customerservice.controller;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController测试类
 * 测试REST API接口功能
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("测试发送聊天消息API")
    void testSendMessage() throws Exception {
        // 准备测试数据
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setUserId("test-user");

        ChatResponse mockResponse = ChatResponse.success("session-123", "Hi there!", "assistant");
        mockResponse.setTokensUsed(50);

        // 模拟服务层响应
        when(chatService.processMessage(any(ChatRequest.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.message").value("Hi there!"))
                .andExpect(jsonPath("$.sender").value("assistant"))
                .andExpect(jsonPath("$.tokensUsed").value(50));
    }

    @Test
    @DisplayName("测试发送空消息")
    void testSendEmptyMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("");
        request.setUserId("test-user");

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试发送无效JSON")
    void testSendInvalidJson() throws Exception {
        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试服务层异常处理")
    void testServiceException() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setUserId("test-user");

        // 模拟服务层抛出异常
        when(chatService.processMessage(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("测试获取会话历史API")
    void testGetSessionHistory() throws Exception {
        // 模拟会话历史数据
        when(chatService.getSessionHistory("session-123"))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/chat/history/session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("测试健康检查API")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Customer Service AI"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("测试会话清理API")
    void testCleanupSessions() throws Exception {
        mockMvc.perform(post("/api/chat/admin/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("会话清理完成"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("测试长消息处理")
    void testLongMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longMessage.append("a");
        }
        request.setMessage(longMessage.toString());
        request.setUserId("test-user");

        ChatResponse mockResponse = ChatResponse.success("session-456", "Message received", "assistant");
        when(chatService.processMessage(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试超长消息拒绝")
    void testTooLongMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        StringBuilder tooLongMessage = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            tooLongMessage.append("a");
        }
        request.setMessage(tooLongMessage.toString());
        request.setUserId("test-user");

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试错误响应格式")
    void testErrorResponseFormat() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setUserId("test-user");

        ChatResponse errorResponse = ChatResponse.error("AI service unavailable");
        when(chatService.processMessage(any(ChatRequest.class))).thenReturn(errorResponse);

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("AI service unavailable"));
    }

    @Test
    @DisplayName("测试Content-Type验证")
    void testContentTypeValidation() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setUserId("test-user");

        // 测试错误的Content-Type
        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("测试HTTP方法验证")
    void testHttpMethodValidation() throws Exception {
        // 测试错误的HTTP方法
        mockMvc.perform(get("/api/chat/message"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/chat/message"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/chat/message"))
                .andExpect(status().isMethodNotAllowed());
    }
}
