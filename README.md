# Spring Alibaba AI 智能客服系统

基于Spring Alibaba AI框架和通义千问大语言模型构建的智能客服系统。

## 功能特性

- 🤖 **智能对话**：集成阿里云通义千问，提供自然流畅的AI对话体验
- 💬 **会话管理**：支持多用户并发会话，自动管理会话生命周期
- 📝 **消息持久化**：完整记录对话历史，支持会话回溯
- 🌐 **RESTful API**：标准化API接口，易于集成
- 🎨 **Web界面**：现代化的聊天界面，响应式设计
- 📊 **系统监控**：健康检查和会话清理功能

## 技术栈

- **Spring Boot 3.2** - 应用框架
- **Spring Alibaba AI** - AI集成框架  
- **通义千问** - 大语言模型
- **Spring Data JPA** - 数据持久化
- **H2 Database** - 内存数据库（演示用）
- **Thymeleaf** - 模板引擎
- **Maven** - 项目管理

## 快速开始

### 前置要求

- Java 17+
- Maven 3.6+
- 阿里云通义千问API密钥

### 配置API密钥

1. 获取阿里云通义千问API密钥
2. 修改 `src/main/resources/application.yml` 文件：

```yaml
spring:
  cloud:
    alibaba:
      ai:
        tongyi:
          api-key: 您的API密钥
```

或者设置环境变量：
```bash
export TONGYI_API_KEY=您的API密钥
```

### 运行应用

1. 克隆项目到本地
2. 进入项目目录
3. 运行以下命令：

```bash
mvn clean install
mvn spring-boot:run
```

4. 访问 http://localhost:8080

## API接口

### 发送消息

```http
POST /api/chat/message
Content-Type: application/json

{
  "message": "您好，我想咨询一下产品信息",
  "sessionId": "可选，会话ID",
  "userId": "可选，用户ID"
}
```

### 获取会话历史

```http
GET /api/chat/history/{sessionId}
```

### 健康检查

```http
GET /api/chat/health
```

## 项目结构

```
src/
├── main/
│   ├── java/com/example/customerservice/
│   │   ├── controller/          # 控制器层
│   │   ├── service/             # 业务逻辑层
│   │   ├── entity/              # 实体类
│   │   ├── repository/          # 数据访问层
│   │   ├── dto/                 # 数据传输对象
│   │   ├── config/              # 配置类
│   │   └── CustomerServiceApplication.java
│   └── resources/
│       ├── templates/           # Thymeleaf模板
│       └── application.yml      # 配置文件
└── test/                        # 测试代码
```

## 配置说明

主要配置项在 `application.yml` 中：

- `customer-service.welcome-message`: 欢迎消息
- `customer-service.system-prompt`: AI系统提示词
- `customer-service.session-timeout`: 会话超时时间（分钟）
- `spring.cloud.alibaba.ai.tongyi.*`: 通义千问相关配置

## 开发指南

### 添加新功能

1. 在相应的包中创建新的类
2. 遵循现有的分层架构
3. 添加相应的测试用例
4. 更新API文档

### 数据库配置

默认使用H2内存数据库，生产环境建议使用MySQL或PostgreSQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/customer_service
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 部署建议

1. 使用Docker容器化部署
2. 配置外部数据库
3. 设置环境变量管理敏感信息
4. 启用生产环境配置

## 常见问题

### Q: 如何获取通义千问API密钥？
A: 访问阿里云官网，注册并开通通义千问服务，在控制台获取API密钥。

### Q: 系统支持哪些数据库？
A: 支持所有JPA兼容的数据库，包括MySQL、PostgreSQL、Oracle等。

### Q: 如何自定义AI回复逻辑？
A: 修改 `ChatService` 类中的 `buildConversationContext` 方法和系统提示词。

### Q: 如何扩展API接口？
A: 在 `ChatController` 中添加新的端点方法，遵循RESTful设计原则。

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交Issue和Pull Request来改进项目。

## 联系方式

如有问题，请通过GitHub Issues联系。
