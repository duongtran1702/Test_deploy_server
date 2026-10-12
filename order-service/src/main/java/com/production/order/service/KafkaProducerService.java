package com.production.order.service;

import com.production.common.dto.OrderEvent;
import com.production.order.config.KafkaTopicConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> publishOrderEvent(OrderEvent event) {
        log.info("[EventPublisher] Publishing OrderEvent to topic '{}', OrderId: {}, TraceId: {}",
                KafkaTopicConfig.ORDERS_TOPIC, event.getOrderId(), event.getCorrelationId());

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopicConfig.ORDERS_TOPIC,
                event.getOrderId(),
                event
        );

        if (event.getCorrelationId() != null) {
            record.headers().add(new RecordHeader("x-correlation-id", event.getCorrelationId().getBytes(StandardCharsets.UTF_8)));
        }
        record.headers().add(new RecordHeader("x-event-type", "ORDER_CREATED".getBytes(StandardCharsets.UTF_8)));

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[EventPublisher] Successfully published OrderEvent to topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[EventPublisher] FAILED to publish OrderEvent for orderId: {}, error: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });

        return future;
    }

    public CompletableFuture<SendResult<String, Object>> replayToOrdersTopic(OrderEvent event) {
        log.warn("[EventReplay] Replaying event back to '{}' for OrderId: {}", KafkaTopicConfig.ORDERS_TOPIC, event.getOrderId());
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopicConfig.ORDERS_TOPIC,
                event.getOrderId(),
                event
        );
        record.headers().add(new RecordHeader("x-replayed", "true".getBytes(StandardCharsets.UTF_8)));
        return kafkaTemplate.send(record);
    }
}
