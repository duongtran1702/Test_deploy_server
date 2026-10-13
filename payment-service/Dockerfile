# Stage 1: Build
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :payment-service:bootJar -x test --no-daemon

# Stage 2: Minimal Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/payment-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
