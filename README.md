# 银保销管智能助手（Insurance Copilot）

基于 **Spring AI + 百炼大模型** 打造的金融保险行业智能问答平台。融合 **RAG 规则检索、Function Calling 查库、多轮对话记忆、企业级权限管控、Redis 热缓存、RabbitMQ 异步削峰**，可直接用于银保销管团队的日常规则咨询与薪资核算。

---

## 一、项目背景

银保渠道的基本法、佣金方案、考核规则文件多、更新频繁，代理人和内勤经常反复咨询同类问题。
本项目用自然语言问答替代"翻文档 + 问内勤"，并能直接调用业务库实时计算薪资、查询业绩，是一个贴近真实生产架构的金融 Copilot 原型。

## 二、核心能力

| 能力 | 说明 |
|---|---|
| RAG 规则问答 | 上传 PDF/Word/TXT 规则文档，大模型只基于检索到的原文回答，无据拒答防幻觉 |
| Function Calling | 大模型自主调用工具：薪资核算、业绩查询、人网关系查询、表结构查询 |
| 多轮对话记忆 | 按 sessionId 持久化上下文，支持"他""那个人"等指代词 |
| 权限管控 | 总管可查全量数据/表结构；机构管理员只能看本机构数据（行级过滤） |
| 敏感词拦截 | 禁止生成/执行删库、删表等破坏性 SQL |
| 审计日志 | 谁、什么角色、问了什么、调了什么工具，独立落盘 |
| 数据脱敏 | 手机号等敏感字段返回前自动脱敏（138****0001） |
| Redis 热缓存 | 对话上下文缓存到 Redis，带 TTL，减少查库 |
| RabbitMQ 削峰 | 对话历史异步落库，手动 ACK + 消息幂等，消息不丢不重复 |

## 三、技术栈

- **后端**：Spring Boot 3.4.5、Spring AI 1.0.0 GA、spring-ai-alibaba
- **大模型**：阿里云百炼 DashScope（qwen-plus + text-embedding-v3）
- **数据库**：MySQL 8.0（业务数据 + 对话历史 + 权限矩阵）
- **中间件**：Redis（热缓存）、RabbitMQ（异步削峰）
- **向量库**：SimpleVectorStore（默认内存），可切换 PGVector
- **文档解析**：Apache Tika
- **构建**：Maven、JDK 17、Docker Compose

## 四、系统架构

```
用户提问
   │
   ▼
RagController（解析登录身份 role/orgId/sessionId）
   │
   ▼
ToolGuard 守卫 ── 敏感词拦截 / 权限校验 / 审计落盘 / 数据脱敏
   │
   ▼
RagService ── ① 向量检索规则文档
            └─ ② 拼接多轮历史（Redis 热缓存）
            └─ ③ 调用大模型，大模型自主决定是否调用 Function Calling 工具
   │
   ├─ Function Calling 工具 ── 薪资/业绩/人网/表结构（SQL 自动按机构过滤）
   ├─ 对话历史 ── RabbitMQ 异步落库（手动ACK + msg_id 幂等）
   └─ 规则文档 ── 向量库检索
```

## 五、快速启动

### 1. 环境准备
- JDK 17、Maven、MySQL 8.0（端口 3306）
- Redis、RabbitMQ（可选，用 Docker 一键起）：
  ```bash
  docker compose up -d
  ```

### 2. 初始化数据库
```sql
-- 建库
CREATE DATABASE mcmsuser;
-- 依次执行 sql/ 目录下：
--   5 张业务表 + sys_tool_permission.sql + chat_history.sql
```

### 3. 配置
修改 `src/main/resources/application.yml`：
- 填入百炼 API Key
- 数据库连接（默认 127.0.0.1:3306/mcmsuser）
- 开关：
  ```yaml
  chat:
    history:
      cache: redis    # memory / redis
      mq: disabled    # disabled / enabled
  ```

### 4. 运行
启动 `InsuranceRagApplication`，端口 8080。

### 5. 接口示例
```bash
# 上传规则文档
POST http://localhost:8080/rag/upload  (form-data: file)

# 多轮问答（总管）
GET http://localhost:8080/rag/chat?q=张伟业绩怎么样&sessionId=u1&role=ADMIN

# 机构管理员（只能看本网点）
GET http://localhost:8080/rag/chat?q=他挂靠哪个网点&sessionId=u1&role=ORG_MANAGER&orgId=1

# 热刷新权限
GET http://localhost:8080/rag/refreshAuth

# 清理 30 天前历史
GET http://localhost:8080/rag/cleanHistory?days=30
```

## 六、目录结构

```
insurance-rag-demo
├── docker-compose.yml              # Redis + RabbitMQ 一键启动
├── pom.xml
├── sql/                             # 建表脚本（业务表、权限表、对话历史表）
└── src/main/java/com/insurance/rag
    ├── InsuranceRagApplication.java # 启动类（@EnableAsync）
    ├── config/
    │   ├── AiConfig.java            # ChatClient + 工具注册 + 向量库
    │   └── RedisConfig.java          # Redis 热缓存（条件装配）
    ├── controller/
    │   └── RagController.java        # 上传/问答/测试/权限刷新/历史清理
    ├── service/
    │   ├── RagService.java           # RAG + 多轮记忆 + 敏感词兜底（核心）
    │   └── ChatHistoryService.java   # 历史读写 + Redis缓存 + 削峰写
    ├── tool/                         # Function Calling 工具
    │   ├── SalaryCalcTool.java       # 薪资核算
    │   ├── QueryPerformanceTool.java  # 业绩查询（行权限+脱敏）
    │   ├── QueryOutletTool.java       # 人网关系查询（行权限）
    │   └── QueryTableTool.java        # 表结构查询（仅总管）
    ├── security/
    │   └── ToolGuard.java            # 权限矩阵(DB加载) + 敏感词 + 审计 + 脱敏
    ├── context/
    │   └── SecurityContext.java       # ThreadLocal 登录上下文
    ├── enums/
    │   └── UserRoleEnum.java         # ADMIN / ORG_MANAGER
    └── mq/
        ├── ChatHistoryMQConfig.java   # RabbitMQ 队列配置
        ├── ChatHistoryProducer.java   # 生产者（JSON + 消息ID）
        └── ChatHistoryConsumer.java    # 消费者（手动ACK + 幂等落库）
```

## 七、设计亮点（面试可讲）

1. **权限双层控制**：功能权限用数据库矩阵管理（热刷新免重启）；行权限在 SQL 自动追加机构 id 过滤，数据库层面隔离，不依赖大模型自觉。
2. **防幻觉**：向量相似度阈值拦截 + system prompt 强制"只依据原文、无据拒答" + 检索片段标注。
3. **高并发治理**：Redis 热缓存（带 TTL）减少查库；RabbitMQ 异步削峰，手动 ACK 保证消息不丢，msg_id 唯一索引保证幂等。
4. **安全兜底**：敏感词在 Java 守卫层强制拦截（不进大模型），提示词只做辅助；手机号等字段返回前脱敏；全链路审计独立落盘。
5. **可插拔**：中间件用 `@ConditionalOnProperty` 开关，本地默认内存版即可跑通，生产一键切 Redis/MQ。

## 八、后续可扩展

- 接入 SpringAI 官方 JdbcChatMemoryRepository 替代手写记忆
- 向量库切换 PGVector 持久化
- 接入 ES 做文档全文检索
- 敏感操作接工作流审批
- 模型私有化部署（Ollama）满足金融数据不出域
