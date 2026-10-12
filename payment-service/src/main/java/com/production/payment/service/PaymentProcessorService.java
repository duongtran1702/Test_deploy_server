package com.production.payment.service;

import com.production.common.dto.OrderEvent;
import com.production.common.dto.PaymentResultEvent;
import com.production.common.model.PaymentStatus;
import com.production.payment.model.PaymentTransaction;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PaymentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorService.class);
    private static final String PAYMENTS_TOPIC = "payments.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyService idempotencyService;
    private final ChaosService chaosService;

    private final ConcurrentMap<String, PaymentTransaction> transactions = new ConcurrentHashMap<>();

    public PaymentProcessorService(KafkaTemplate<String, Object> kafkaTemplate,
                                   IdempotencyService idempotencyService,
                                   ChaosService chaosService) {
        this.kafkaTemplate = kafkaTemplate;
        this.idempotencyService = idempotencyService;
        this.chaosService = chaosService;
    }

    public PaymentResultEvent processOrderPayment(OrderEvent orderEvent, int retryAttempt) {
        log.info("[PaymentProcessor] Processing order payment. OrderId: {}, Customer: {}, Amount: {}, Attempt: {}",
                orderEvent.getOrderId(), orderEvent.getCustomerId(), orderEvent.getAmount(), retryAttempt);

        // 1. Idempotency Check
        if (idempotencyService.isAlreadyProcessed(orderEvent.getOrderId())) {
            log.warn("[PaymentProcessor] Skipping duplicate processing for OrderId: {}", orderEvent.getOrderId());
            PaymentTransaction existing = transactions.get(orderEvent.getOrderId());
            return new PaymentResultEvent(
                    existing != null ? existing.getPaymentId() : UUID.randomUUID().toString(),
                    orderEvent.getOrderId(),
                    orderEvent.getCustomerId(),
                    orderEvent.getAmount(),
                    PaymentStatus.SUCCESS,
                    "Idempotent response: payment already completed previously",
                    orderEvent.getCorrelationId(),
                    Instant.now(),
                    retryAttempt,
                    "payment-service (idempotent-cache)"
            );
        }

        // 2. Chaos & Fault Injection Simulation
        chaosService.applyChaos(orderEvent);

        // 3. Mark idempotency & record transaction
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        idempotencyService.markAsProcessed(orderEvent.getOrderId());

        PaymentTransaction tx = new PaymentTransaction(
                paymentId,
                orderEvent.getOrderId(),
                orderEvent.getCustomerId(),
                orderEvent.getAmount(),
                PaymentStatus.SUCCESS,
                "Payment authorized and settled successfully",
                retryAttempt,
                orderEvent.getCorrelationId()
        );
        transactions.put(orderEvent.getOrderId(), tx);

        // 4. Construct Result Event
        PaymentResultEvent resultEvent = new PaymentResultEvent(
                paymentId,
                orderEvent.getOrderId(),
                orderEvent.getCustomerId(),
                orderEvent.getAmount(),
                PaymentStatus.SUCCESS,
                "Payment authorized and settled",
                orderEvent.getCorrelationId(),
                Instant.now(),
                retryAttempt,
                "payment-service"
        );

        // 5. Emit to Kafka payments.v1
        publishPaymentResult(resultEvent);
        return resultEvent;
    }

    private void publishPaymentResult(PaymentResultEvent event) {
        log.info("[PaymentPublisher] Publishing PaymentResult to '{}' for OrderId: {}, Status: {}",
                PAYMENTS_TOPIC, event.getOrderId(), event.getStatus());

        ProducerRecord<String, Object> record = new ProducerRecord<>(PAYMENTS_TOPIC, event.getOrderId(), event);
        if (event.getCorrelationId() != null) {
            record.headers().add(new RecordHeader("x-correlation-id", event.getCorrelationId().getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record).whenComplete((res, ex) -> {
            if (ex == null) {
                log.info("[PaymentPublisher] Emitted payment result successfully for OrderId: {}", event.getOrderId());
            } else {
                log.error("[PaymentPublisher] Failed to emit payment result for OrderId: {}, error: {}", event.getOrderId(), ex.getMessage());
            }
        });
    }

    public List<PaymentTransaction> getAllTransactions() {
        List<PaymentTransaction> list = new ArrayList<>(transactions.values());
        list.sort(Comparator.comparing(PaymentTransaction::getCreatedAt).reversed());
        return list;
    }

    public Map<String, Object> getMetrics() {
        long total = transactions.size();
        long success = transactions.values().stream().filter(t -> t.getStatus() == PaymentStatus.SUCCESS).count();
        long declined = transactions.values().stream().filter(t -> t.getStatus() == PaymentStatus.DECLINED).count();

        Map<String, Object> map = new HashMap<>();
        map.put("totalTransactions", total);
        map.put("successfulTransactions", success);
        map.put("declinedTransactions", declined);
        map.put("chaosConfig", chaosService.getConfig());
        return map;
    }

    public void clear() {
        transactions.clear();
        idempotencyService.clear();
    }
}
