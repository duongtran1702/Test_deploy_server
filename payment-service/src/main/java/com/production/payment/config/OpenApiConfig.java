package com.production.payment.config;

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
                        .title("Payment Service API (Production Microservices)")
                        .version("1.0.0")
                        .description("Microservice processing Kafka order events, managing idempotency, exponential retries, DLT routing, and chaos engineering simulations.")
                        .contact(new Contact().name("Engineering Team").email("dev@production.local"))
                        .license(new License().name("Apache 2.0")));
    }
}
