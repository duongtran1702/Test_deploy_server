package com.production.order.controller;

import com.production.common.dto.OrderRequest;
import com.production.common.dto.OrderResponse;
import com.production.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller", description = "Endpoints for creating and managing distributed orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new Order", description = "Validates input and publishes an OrderEvent to Kafka orders.v1")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Returns all orders sorted newest first")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get order metrics and health summary")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(orderService.getMetrics());
    }

    @PostMapping("/{id}/test-sync-payment")
    @Operation(summary = "Test direct REST payment call with Circuit Breaker")
    public ResponseEntity<Map<String, Object>> testDirectPayment(@PathVariable String id) {
        Map<String, Object> result = orderService.testDirectPayment(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    @Operation(summary = "Generate a batch of sample orders for load/chaos testing")
    public ResponseEntity<List<OrderResponse>> createBatchOrders(@RequestParam(defaultValue = "5") int count,
                                                                 @RequestParam(defaultValue = "0") int failurePercent) {
        List<OrderResponse> results = new ArrayList<>();
        String[] items = {"MacBook Pro M3", "Dell XPS 15", "Sony WH-1000XM5", "Keychron Q1 Pro", "LG UltraFine 4K"};
        String[] customers = {"Alice Nguyen", "Bob Tran", "Charlie Le", "David Pham", "Emma Vu"};

        for (int i = 0; i < count; i++) {
            boolean shouldFail = (Math.random() * 100) < failurePercent;
            boolean shouldPoison = shouldFail && (i % 2 == 0);

            OrderRequest req = new OrderRequest(
                    customers[i % customers.length],
                    items[i % items.length],
                    BigDecimal.valueOf(100 + (i * 50)),
                    shouldFail,
                    shouldPoison
            );
            results.add(orderService.createOrder(req));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    @DeleteMapping
    @Operation(summary = "Clear all orders from in-memory repository")
    public ResponseEntity<Map<String, String>> clearOrders() {
        orderService.clearAll();
        return ResponseEntity.ok(Map.of("message", "All orders cleared"));
    }
}
