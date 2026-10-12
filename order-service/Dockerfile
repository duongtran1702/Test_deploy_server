FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/order-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
