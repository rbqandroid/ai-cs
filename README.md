# Spring Alibaba AI 智能客服系统

基于Spring Alibaba AI框架和DashScope大语言模型构建的智能客服系统，集成了完整的知识库管理功能。

## 🌟 功能特性

### 核心功能
- 🤖 **智能对话**：集成阿里云DashScope，提供自然流畅的AI对话体验
- 💬 **会话管理**：支持多用户并发会话，自动管理会话生命周期
- 📝 **消息持久化**：完整记录对话历史，支持会话回溯
- 🌐 **RESTful API**：标准化API接口，易于集成
- 🎨 **Web界面**：现代化的聊天界面，响应式设计
- 📊 **系统监控**：健康检查和会话清理功能

### 知识库功能 🆕
- 📚 **知识库管理**：支持文档的创建、编辑、发布、归档
- 🗂️ **分类管理**：层级分类系统，支持无限级分类
- 🔍 **智能搜索**：关键词搜索和语义搜索（规划中）
- 🤖 **AI增强回答**：基于知识库内容生成更准确的回答
- 📈 **访问统计**：文档访问次数、点赞数等统计功能
- 🏷️ **标签系统**：支持文档标签管理和标签搜索

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 3.3.3** - 应用框架
- **Spring Alibaba AI 1.0.0.2** - AI集成框架
- **DashScope** - 阿里云大语言模型服务
- **Spring Data JPA** - 数据持久化
- **H2 Database** - 内存数据库（演示用）
- **Thymeleaf** - 模板引擎
- **Maven** - 项目管理
- **Java 21** - 编程语言

### AI功能
- **Spring AI** - AI应用开发框架
- **智能降级机制** - API密钥未配置时使用模拟AI
- **知识库搜索** - 基于关键词的文档搜索
- **上下文增强** - 结合知识库内容生成回答

## 🚀 快速开始

### 前置要求

- **Java 21+** - 推荐使用OpenJDK 21
- **Maven 3.6+** - 项目构建工具
- **阿里云DashScope API密钥**（可选）- 用于真实AI功能

### 1. 克隆和编译项目

```bash
# 克隆项目
git clone <repository-url>
cd spring-alibaba-ai-customer-service

# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### 2. 配置API密钥（可选）

如果要使用真实的AI功能，需要配置DashScope API密钥：

1. 获取阿里云DashScope API密钥
2. 设置环境变量：

```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

或者修改 `src/main/resources/application.yml` 文件：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
```

> 💡 **提示**：如果不配置API密钥，系统会自动使用智能模拟AI，仍然可以正常体验所有功能。

### 3. 访问应用

- **主页**: http://localhost:8080
- **聊天界面**: http://localhost:8080
- **API健康检查**: http://localhost:8080/api/chat/health
- **H2数据库控制台**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - 用户名: `sa`
  - 密码: 空

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
