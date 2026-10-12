package com.production.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDERS_TOPIC = "orders.v1";
    public static final String PAYMENTS_TOPIC = "payments.v1";
    public static final String ORDERS_DLT = "orders.v1.DLT";

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ORDERS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(PAYMENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersDltTopic() {
        return TopicBuilder.name(ORDERS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
