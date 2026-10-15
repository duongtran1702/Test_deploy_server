# Stage 1: Build Jar
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :payment-service:bootJar -x test --no-daemon

# Stage 2: Runtime Non-Root (Temurin 21 JRE)
FROM eclipse-temurin:21-jre-alpine

# Bắt buộc cho đề thi: Chạy dưới quyền user non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /app/payment-service/build/libs/*[!plain].jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
