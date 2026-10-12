package com.production.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API (Production Microservices)")
                        .version("1.0.0")
                        .description("Microservice managing orders, Kafka event streaming, Circuit Breaker resilience, and Dead Letter Topic monitoring.")
                        .contact(new Contact().name("Engineering Team").email("dev@production.local"))
                        .license(new License().name("Apache 2.0")));
    }
}
