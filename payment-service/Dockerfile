FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/payment-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
