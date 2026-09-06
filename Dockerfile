# =============================================================================
# 统一多阶段构建（所有服务共用）
#   docker build --build-arg MODULE=ai-saas-chat-service -t ai-saas/chat:latest .
# =============================================================================

# ---- 构建阶段: Maven + JDK17（依赖层缓存优化: 先拷 pom 单独下载依赖）----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

ARG MODULE=ai-saas-chat-service

COPY pom.xml .
COPY ai-saas-common/pom.xml ai-saas-common/
COPY ai-saas-gateway/pom.xml ai-saas-gateway/
COPY ai-saas-user-service/pom.xml ai-saas-user-service/
COPY ai-saas-chat-service/pom.xml ai-saas-chat-service/
COPY ai-saas-task-service/pom.xml ai-saas-task-service/
COPY ai-saas-rag-service/pom.xml ai-saas-rag-service/
COPY ai-saas-billing-service/pom.xml ai-saas-billing-service/
COPY ai-saas-admin-service/pom.xml ai-saas-admin-service/

# 先只下依赖, 这层在 pom 不变时走缓存
RUN mvn -B dependency:go-offline -q || true

COPY . .
# 只构建目标模块及其依赖(common), 跳过测试
RUN mvn -B clean package -DskipTests -pl ${MODULE} -am -q

# ---- 运行阶段: 精简 JRE ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG MODULE=ai-saas-chat-service
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=builder /build/${MODULE}/target/*.jar app.jar

# 非 root 运行
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
