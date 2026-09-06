# ============================================================
# SmartSlot Backend Multi-Stage Dockerfile (Spring Boot 3)
# ============================================================

# Stage 1: 构建阶段
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# 缓存依赖层
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 编译打包应用程序
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: 极致轻量化运行镜像 (JRE 17 Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 设置容器时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# 拷贝 Spring Boot 可执行 jar
COPY --from=builder /build/target/smart-slot-backend-1.0.0.jar app.jar

EXPOSE 8088

# 生产级 JVM 调优参数与熵池提速
ENTRYPOINT ["java", \
    "-server", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
