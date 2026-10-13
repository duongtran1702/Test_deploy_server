package com.production.order.service;

import com.production.common.dto.OrderEvent;
import com.production.common.dto.OrderRequest;
import com.production.common.dto.OrderResponse;
import com.production.common.model.OrderStatus;
import com.production.order.model.OrderEntity;
import com.production.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;
    private final PaymentEventListener paymentEventListener;
    private final PaymentRestClient paymentRestClient;

    public OrderService(OrderRepository orderRepository,
                        KafkaProducerService kafkaProducerService,
                        PaymentEventListener paymentEventListener,
                        PaymentRestClient paymentRestClient) {
        this.orderRepository = orderRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.paymentEventListener = paymentEventListener;
        this.paymentRestClient = paymentRestClient;
    }

    public OrderResponse createOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String correlationId = Optional.ofNullable(MDC.get("correlationId"))
                .orElse(UUID.randomUUID().toString());

        log.info("[OrderService] Initiating order: {}, customer: {}, amount: {}",
                orderId, request.getCustomerId(), request.getAmount());

        OrderEntity entity = new OrderEntity(
                orderId,
                request.getCustomerId(),
                request.getItem(),
                request.getAmount(),
                OrderStatus.PENDING,
                correlationId
        );
        entity.setNote("Order submitted, queued to Kafka topic: orders.v1");
        orderRepository.save(entity);

        // Prepare Kafka event
        OrderEvent event = new OrderEvent(
                orderId,
                request.getCustomerId(),
                request.getItem(),
                request.getAmount(),
                OrderStatus.PENDING,
                correlationId,
                Instant.now(),
                request.isSimulatePoisonPill(),
                request.isSimulateFailure()
        );

        // Publish to Kafka asynchronously
        try {
            kafkaProducerService.publishOrderEvent(event);
        } catch (Exception ex) {
            log.error("[OrderService] Kafka publish failure: {}", ex.getMessage());
            entity.setLastEventError("Kafka Broker Error: " + ex.getMessage());
            orderRepository.save(entity);
        }

        return toResponse(entity);
    }

    public Map<String, Object> testDirectPayment(String orderId) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return paymentRestClient.executeDirectPayment(entity.getId(), entity.getAmount(), entity.getCustomerId());
    }

    public Optional<OrderResponse> getOrderById(String id) {
        return orderRepository.findById(id).map(this::toResponse);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public boolean replayOrder(String orderId) {
        Optional<OrderEntity> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) return false;

        OrderEntity order = opt.get();
        order.setStatus(OrderStatus.PENDING);
        order.setNote("Order replayed from DLT at " + Instant.now());
        order.setLastEventError(null);
        orderRepository.save(order);

        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getCustomerId(),
                order.getItem(),
                order.getAmount(),
                OrderStatus.PENDING,
                order.getCorrelationId(),
                Instant.now(),
                false, // Reset poison pill on replay
                false  // Reset failure flag on replay
        );

        paymentEventListener.removeDltRecord(orderId);
        kafkaProducerService.replayToOrdersTopic(event);
        return true;
    }

    public Optional<OrderResponse> cancelOrder(String id) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setNote("Đơn hàng đã bị hủy bởi quản trị viên (Admin)");
            orderRepository.save(order);
            log.info("[OrderService] Order {} cancelled by Admin", id);
            return toResponse(order);
        });
    }

    public Optional<OrderResponse> shipOrder(String id) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(OrderStatus.SHIPPED);
            order.setNote("Đơn hàng đã được xuất kho và bàn giao vận chuyển");
            orderRepository.save(order);
            log.info("[OrderService] Order {} dispatched/shipped by Admin", id);
            return toResponse(order);
        });
    }

    public Map<String, Object> getMetrics() {
        List<OrderEntity> all = orderRepository.findAll();
        long total = all.size();
        long confirmed = all.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long pending = all.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long failed = all.stream().filter(o -> o.getStatus() == OrderStatus.FAILED).count();
        long dlt = all.stream().filter(o -> o.getStatus() == OrderStatus.DLT_ROUTED).count();
        long shipped = all.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
        long cancelled = all.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrders", total);
        metrics.put("confirmedOrders", confirmed);
        metrics.put("pendingOrders", pending);
        metrics.put("failedOrders", failed);
        metrics.put("dltRoutedOrders", dlt);
        metrics.put("shippedOrders", shipped);
        metrics.put("cancelledOrders", cancelled);
        metrics.put("successRate", total > 0 ? (double) (confirmed + shipped) / total * 100 : 0.0);
        return metrics;
    }

    public void clearAll() {
        orderRepository.clear();
    }

    private OrderResponse toResponse(OrderEntity e) {
        return new OrderResponse(
                e.getId(),
                e.getCustomerId(),
                e.getItem(),
                e.getAmount(),
                e.getStatus(),
                e.getCorrelationId(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getNote()
        );
    }
}
