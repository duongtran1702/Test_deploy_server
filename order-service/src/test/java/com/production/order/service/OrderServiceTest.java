package com.production.order.service;

import com.production.common.dto.OrderRequest;
import com.production.common.dto.OrderResponse;
import com.production.common.model.OrderStatus;
import com.production.order.model.OrderEntity;
import com.production.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private PaymentEventListener paymentEventListener;

    @Mock
    private PaymentRestClient paymentRestClient;

    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        orderService = new OrderService(orderRepository, kafkaProducerService, paymentEventListener, paymentRestClient);
    }

    @Test
    void testCreateOrder_Success() {
        OrderRequest request = new OrderRequest("Alice", "MacBook Pro", BigDecimal.valueOf(1999.99), false, false);

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals("Alice", response.getCustomerId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, orderRepository.count());

        verify(kafkaProducerService, times(1)).publishOrderEvent(any());
    }

    @Test
    void testGetMetrics() {
        orderService.createOrder(new OrderRequest("Alice", "Item 1", BigDecimal.valueOf(100), false, false));
        orderService.createOrder(new OrderRequest("Bob", "Item 2", BigDecimal.valueOf(200), false, false));

        Map<String, Object> metrics = orderService.getMetrics();

        assertEquals(2L, metrics.get("totalOrders"));
        assertEquals(2L, metrics.get("pendingOrders"));
    }

    @Test
    void testReplayOrder() {
        OrderResponse created = orderService.createOrder(new OrderRequest("Alice", "Item 1", BigDecimal.valueOf(100), false, false));
        
        // Simulate marked as DLT
        OrderEntity entity = orderRepository.findById(created.getOrderId()).orElseThrow();
        entity.setStatus(OrderStatus.DLT_ROUTED);
        orderRepository.save(entity);

        boolean replayed = orderService.replayOrder(created.getOrderId());

        assertTrue(replayed);
        OrderEntity updated = orderRepository.findById(created.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, updated.getStatus());
        verify(kafkaProducerService, times(1)).replayToOrdersTopic(any());
    }
}
