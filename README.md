# AI SaaS Backend Platform

基于 **Spring Cloud Alibaba** 的企业级 AI SaaS 后端微服务平台，覆盖网关治理、服务间调用、分布式一致性、高并发缓存、数据库性能治理与 RAG 检索增强全链路。

## 技术栈

| 分类 | 技术 |
|---|---|
| 语言 / 框架 | Java 17 · Spring Boot 3.2.5 · Spring Cloud Alibaba 2023 |
| 微服务治理 | Nacos（注册/配置） · Spring Cloud Gateway · Sentinel（限流/熔断） · OpenFeign |
| 数据存储 | MySQL 8 · Redis 7 / Redisson（缓存、分布式锁、布隆过滤器） · Qdrant（向量检索，可选） |
| 消息队列 | RocketMQ 5.x（异步解耦、事务消息、削峰填谷） |
| AI 能力 | LangChain4j · OpenAI / DeepSeek · SSE 流式对话 |
| 工程化 | Maven 多模块 · Docker / docker-compose · GitHub Actions CI |

## 系统架构

网关 + 6 个业务微服务，按业务域垂直拆分：

| 模块 | 端口 | 职责 |
|---|---|---|
| `ai-saas-gateway` | 8080 | 统一接入：JWT 鉴权、IP 黑白名单、限流熔断、可信身份传播、traceId 全链路透传 |
| `ai-saas-user-service` | 8081 | 注册登录、验证码、OAuth 绑定、用户资料；用户缓存 + 布隆过滤器 |
| `ai-saas-chat-service` | 8082 | SSE 流式对话、多模型抽象与故障降级、会话管理 |
| `ai-saas-task-service` | 8083 | 异步任务引擎：状态机、责任链 + 工厂、重试/超时/SSE 进度 |
| `ai-saas-billing-service` | 8084 | 配额扣减、Token 计费、Redis 幂等 + DB CAS + 对账 |
| `ai-saas-rag-service` | 8086 | RAG 全链路：文档解析 → 切片 → Embedding → 向量检索 |
| `ai-saas-common` | — | 公共组件：统一响应、全局异常、Redis/线程池工具、AI 抽象、向量存储 |
| `ai-saas-admin-service` | — | 管理端（预留） |

## 核心设计与实现

**微服务治理**：Nacos 注册发现与配置中心；Gateway 统一收敛鉴权、黑白名单、限流熔断；设计可信身份传播链路（网关签发 → 服务端校验防伪造），traceId 贯穿全链路。

**调用架构与容错**：按数据一致性诉求拆分——强一致读经 OpenFeign 同步（1s/2s 紧超时、fail-open 降级、上下文透传），可异步写经 RocketMQ 解耦；内部接口隔离在 `/internal/**` 不对公网开放。

**分布式一致性**：RocketMQ 半消息事务绑定本地业务，消费端 Redis 幂等标记 + 数据库 CAS 双保险防重，死信队列与定时对账兜底，保证计费零重复、零丢失。

**异步任务引擎**：状态机驱动任务生命周期（终态保护、非法迁移拦截），责任链 + 工厂模式解耦处理流程，MQ 削峰填谷。

**MySQL 性能治理**：100 万行 / 400 用户压测下定位分页慢查询根因（6 个单列索引诱发 index_merge + filesort），按"查询谓词与排序键对齐"原则重构联合索引，扫描行 2384→20、P95 ~500ms→~2ms（约 230 倍）。详见 [docs/MySQL优化案例](./docs/MySQL优化案例-会话列表分页索引.md)。

**Redis 缓存体系**：Cache-Aside + 命名空间级差异化 TTL；布隆过滤器前置拦截穿透、`@Cacheable(sync=true)` 单飞防击穿、错峰 TTL 防雪崩；写路径按业务键精确失效。详见 [docs/缓存设计](./docs/缓存设计.md)。

**AI 能力**：多模型抽象与故障降级路由、SSE 流式对话与生成中止、RAG 检索增强、Agent Workflow 工作流定义引擎；Qdrant 不可用时自动降级为内存向量库。

## 环境要求

- JDK 17
- Maven 3.6+
- Docker（用于启动 MySQL / Redis / Nacos / RocketMQ）

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

包含：MySQL 8（3306）、Redis 7（6379）、Nacos 2.3（8848/9848）、RocketMQ 5.1（9876/10911）。

### 2. 构建

```bash
mvn clean package -DskipTests
```

### 3. 启动服务

各模块启动类位于 `ai-saas-<module>/src/main/java` 下，按依赖顺序启动（业务服务可并行，网关最后）：

```bash
java -jar ai-saas-user-service/target/ai-saas-user-service-1.0.0-SNAPSHOT.jar
java -jar ai-saas-chat-service/target/ai-saas-chat-service-1.0.0-SNAPSHOT.jar
java -jar ai-saas-task-service/target/ai-saas-task-service-1.0.0-SNAPSHOT.jar
java -jar ai-saas-billing-service/target/ai-saas-billing-service-1.0.0-SNAPSHOT.jar
java -jar ai-saas-rag-service/target/ai-saas-rag-service-1.0.0-SNAPSHOT.jar
java -jar ai-saas-gateway/target/ai-saas-gateway-1.0.0-SNAPSHOT.jar
```

### 4. 健康检查

```bash
curl http://localhost:8080/actuator/health   # 网关
curl http://localhost:8081/actuator/health   # 用户服务
# ... 其余端口见上表
```

## 目录结构

```
ai-saas-backend-platform/
├── ai-saas-gateway/          # 统一网关
├── ai-saas-user-service/     # 用户域
├── ai-saas-chat-service/     # 对话域
├── ai-saas-task-service/     # 异步任务
├── ai-saas-billing-service/  # 计费域
├── ai-saas-rag-service/      # 知识库 RAG
├── ai-saas-admin-service/    # 管理端（预留）
├── ai-saas-common/           # 公共组件
├── docker/mysql/init/        # MySQL 初始化脚本
├── .github/workflows/        # CI
└── docs/                     # 设计文档与性能案例
```

## 测试

```bash
mvn test
```

43 个单元/集成测试（JUnit 5 + Mockito），含 Spring 上下文缓存失效验证。
