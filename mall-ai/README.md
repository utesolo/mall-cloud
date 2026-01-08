# mall-ai AI智能服务模块

基于Spring AI + DashScope + Qdrant + PyMuPDF4LLM实现的Agent + RAG系统

## 📦 项目概述

mall-ai是农业供销平台的AI智能服务模块，提供基于RAG（检索增强生成）的文档智能问答功能。用户可以上传PDF文档到知识库，系统会自动进行向量化处理，然后通过Agent智能体实现基于文档内容的智能对话和问答。

## ✅ 已完成功能

### 1. 模块基础架构 ✓
- ✅ Maven多模块配置
- ✅ Spring Boot 3.5.8 + Spring Cloud 2025.0.0
- ✅ Nacos服务注册与配置中心集成
- ✅ 服务端口：8090

### 2. 数据库设计 ✓
已创建完整的数据库表结构（`sql/mall_ai.sql`）：
- ✅ `ai_document` - 文档表
- ✅ `ai_document_chunk` - 文档块表
- ✅ `ai_document_chunk` - 对话会话表
- ✅ `ai_chat_message` - 对话消息表

### 3. 实体层与数据访问层 ✓
- ✅ 完整的Entity实体类（支持MyBatis-Plus）
- ✅ Mapper接口定义
- ✅ 逻辑删除支持

### 4. DTO和VO类 ✓
**请求DTO：**
- ✅ `DocumentUploadDTO` - 文档上传
- ✅ `DocumentQueryDTO` - 文档查询
- ✅ `ChatRequestDTO` - 对话请求

**响应VO：**
- ✅ `DocumentVO` - 文档信息
- ✅ `DocumentStatusVO` - 处理状态
- ✅ `ChatResponseVO` - 对话响应
- ✅ `ChunkReferenceVO` - 文档引用
- ✅ `TokenUsageVO` - Token使用情况
- ✅ `SessionHistoryVO` - 会话历史
- ✅ `MessageVO` - 消息详情

### 5. 配置管理 ✓
**配置类：**
- ✅ `AiProperties` - AI配置属性
- ✅ `RagProperties` - RAG参数配置
- ✅ `AgentProperties` - Agent参数配置
- ✅ `SpringAiConfig` - Spring AI配置

**Nacos配置文件：**
- ✅ `mall-ai-config.yml` - AI服务配置
- ✅ `mall-ai-mysql.yml` - 数据库配置

### 6. PyMuPDF解析服务 ✓
已创建独立的Python服务（`pymupdf-service/`）：
- ✅ Flask HTTP服务
- ✅ PyMuPDF4LLM集成
- ✅ 文件上传和URL解析支持
- ✅ 健康检查接口
- ✅ 完整的README文档

## 🏗️ 技术架构

```
mall-ai (Java微服务)
├── Entity层 - MyBatis-Plus实体
├── Mapper层 - 数据访问接口
├── DTO/VO层 - 请求响应对象
├── Config层 - 配置类
├── Service层 - 业务逻辑（待实现）
└── Controller层 - REST接口（待实现）

pymupdf-service (Python服务)
└── Flask HTTP服务 - PDF解析
```

## 📋 待实现功能

由于时间限制，以下核心业务逻辑需要后续补充实现：

### Service层（业务服务）
- ⏳ `VectorStoreService` - 向量存储服务
  - Qdrant向量操作封装
  - 向量增删改查
  - 相似度检索

- ⏳ `PyMuPDFClientService` - PyMuPDF客户端
  - WebClient调用Python服务
  - 文件上传和解析
  - 异常处理和重试

- ⏳ `DocumentService` - 文档处理服务
  - PDF上传处理
  - 文档分块（TokenTextSplitter）
  - 向量化和存储
  - 文档查询和删除

- ⏳ `RAGService` - 检索增强服务
  - 向量检索
  - 上下文构建
  - 重排序和过滤

- ⏳ `AgentService` - 智能体服务
  - DashScope大模型调用
  - Prompt构建
  - 会话管理
  - 流式输出支持

### Controller层（REST接口）
- ⏳ `DocumentController` - 文档管理
  - POST `/api/ai/documents/upload` - 上传文档
  - GET `/api/ai/documents` - 查询文档列表
  - DELETE `/api/ai/documents/{id}` - 删除文档
  - GET `/api/ai/documents/{id}/status` - 查询处理状态

- ⏳ `AgentController` - Agent对话
  - POST `/api/ai/agent/chat` - 发起对话
  - POST `/api/ai/agent/chat/stream` - 流式对话
  - GET `/api/ai/agent/sessions/{sessionKey}/history` - 查询历史
  - DELETE `/api/ai/agent/sessions/{sessionKey}` - 清空会话

### 其他待完善
- ⏳ 网关路由配置（mall-gateway）
- ⏳ 单元测试和集成测试
- ⏳ API文档完善（Swagger/OpenAPI）
- ⏳ 异常处理和日志AOP
- ⏳ 限流和熔断配置

## 🚀 部署指南

### 1. 数据库初始化

```bash
# 执行SQL脚本创建数据库和表
mysql -u root -p < sql/mall_ai.sql
```

### 2. 启动Qdrant向量数据库

```bash
# 使用Docker运行Qdrant
docker run -p 6333:6333 qdrant/qdrant
```

### 3. 启动PyMuPDF服务

```bash
cd pymupdf-service
pip install -r requirements.txt
python pymupdf_service.py
```

### 4. 配置Nacos

将以下配置文件导入Nacos配置中心：
- `nacos-config/mall-ai-config.yml` → Data ID: `mall-ai.yml`
- `nacos-config/mall-ai-mysql.yml` → Data ID: `mall-ai-mysql.yml`

需要配置的环境变量：
- `DASHSCOPE_API_KEY` - 阿里云DashScope API密钥
- `QDRANT_HOST` - Qdrant服务地址
- `PYMUPDF_SERVICE_URL` - PyMuPDF服务URL

### 5. 启动mall-ai服务

```bash
cd mall-ai
mvn spring-boot:run
```

服务将在 `http://localhost:8090` 启动

## 📝 开发规范

本项目遵循以下开发规范：

1. **代码规范**：遵循AGENTS.md中的编码规范
2. **JavaDoc**：每个方法必须有完整的JavaDoc注释
3. **参数封装**：超过3个参数必须封装为DTO
4. **返回值**：Controller统一返回`Result<T>`格式
5. **日志**：使用`@Slf4j`注解，通过AOP统一处理
6. **异常处理**：使用统一异常处理器
7. **数据库操作**：使用MyBatis-Plus的LambdaQueryWrapper

## 🔗 相关文档

- [设计文档](../.qoder/quests/pdf-vector-storage-agent.md)
- [项目AGENTS规范](../AGENTS.md)
- [PyMuPDF服务README](../pymupdf-service/README.md)

## 🤝 后续开发建议

1. **优先实现核心Service层**
   - 先实现`DocumentService`的基本功能
   - 再实现`RAGService`和`AgentService`
   - 最后实现Controller层

2. **集成Spring AI**
   - 根据实际的Spring AI版本调整配置
   - 实现DashScope的ChatModel和EmbeddingModel
   - 配置Qdrant VectorStore

3. **测试和优化**
   - 编写单元测试
   - 性能测试和优化
   - 异常场景测试

4. **文档完善**
   - 补充API文档
   - 编写使用示例
   - 部署和运维文档

## 📞 技术支持

如有问题，请参考：
- Spring AI官方文档：https://docs.spring.io/spring-ai/
- Qdrant文档：https://qdrant.tech/documentation/
- PyMuPDF4LLM：https://github.com/pymupdf/PyMuPDF

---

**版本**：1.0.0  
**最后更新**：2026-01-08  
**作者**：mall-cloud团队
