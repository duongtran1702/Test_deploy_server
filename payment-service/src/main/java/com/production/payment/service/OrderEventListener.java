package com.production.payment.service;

import com.production.common.dto.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final PaymentProcessorService paymentProcessorService;

    public OrderEventListener(PaymentProcessorService paymentProcessorService) {
        this.paymentProcessorService = paymentProcessorService;
    }

    @KafkaListener(
            topics = "orders.v1",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderEvent(ConsumerRecord<String, OrderEvent> record,
                             @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer deliveryAttempt) {

        OrderEvent event = record.value();
        int attempt = deliveryAttempt != null ? deliveryAttempt : 1;

        String correlationId = event.getCorrelationId();
        if (record.headers().lastHeader("x-correlation-id") != null) {
            correlationId = new String(record.headers().lastHeader("x-correlation-id").value(), StandardCharsets.UTF_8);
        }
        MDC.put("correlationId", correlationId);

        log.info("[KafkaConsumer] Received OrderEvent from topic: {}, partition: {}, offset: {}, key: {}, attempt: {}",
                record.topic(), record.partition(), record.offset(), record.key(), attempt);

        try {
            paymentProcessorService.processOrderPayment(event, attempt);
            log.info("[KafkaConsumer] OrderEvent successfully processed for OrderId: {}", event.getOrderId());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
