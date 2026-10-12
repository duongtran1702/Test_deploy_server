package com.production.order.service;

import com.production.common.dto.OrderEvent;
import com.production.common.dto.PaymentResultEvent;
import com.production.common.model.OrderStatus;
import com.production.common.model.PaymentStatus;
import com.production.order.model.OrderEntity;
import com.production.order.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderRepository orderRepository;
    private final Map<String, ConsumerRecord<String, Object>> deadLetterRecords = new ConcurrentHashMap<>();

    public PaymentEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payments.v1", groupId = "order-service-payment-group")
    public void handlePaymentResult(PaymentResultEvent paymentResult) {
        log.info("[KafkaConsumer] Received payment result for order: {}, status: {}, reason: {}",
                paymentResult.getOrderId(), paymentResult.getStatus(), paymentResult.getReason());

        orderRepository.findById(paymentResult.getOrderId()).ifPresentOrElse(order -> {
            if (paymentResult.getStatus() == PaymentStatus.SUCCESS) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setNote("Payment completed successfully by " + paymentResult.getProcessedByService());
            } else {
                order.setStatus(OrderStatus.FAILED);
                order.setNote("Payment rejected: " + paymentResult.getReason());
            }
            orderRepository.save(order);
            log.info("[OrderStatusUpdate] Order {} status updated to {}", order.getId(), order.getStatus());
        }, () -> {
            log.warn("[KafkaConsumer] Order not found for payment result: {}", paymentResult.getOrderId());
        });
    }

    @KafkaListener(topics = "orders.v1.DLT", groupId = "order-service-dlt-monitor-group")
    public void handleDeadLetterTopic(ConsumerRecord<String, Object> record) {
        String key = record.key() != null ? record.key() : "unknown-" + System.currentTimeMillis();
        log.warn("[DLTMonitor] Message detected in Dead Letter Topic! Key: {}, Partition: {}, Offset: {}",
                key, record.partition(), record.offset());

        deadLetterRecords.put(key, record);

        orderRepository.findById(key).ifPresent(order -> {
            order.setStatus(OrderStatus.DLT_ROUTED);
            order.setNote("Routed to Dead Letter Topic (DLT) after retries exhausted or poison pill detected.");
            order.setLastEventError("Exceeded retry policy in Kafka Consumer");
            orderRepository.save(order);
        });
    }

    public List<ConsumerRecord<String, Object>> getDeadLetterRecords() {
        return new ArrayList<>(deadLetterRecords.values());
    }

    public void removeDltRecord(String key) {
        deadLetterRecords.remove(key);
    }
}
