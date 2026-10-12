package com.production.order.controller;

import com.production.order.service.OrderService;
import com.production.order.service.PaymentEventListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/dlt")
@Tag(name = "DLT Controller", description = "Monitor and replay Dead Letter Topic events")
public class DltController {

    private final PaymentEventListener paymentEventListener;
    private final OrderService orderService;

    public DltController(PaymentEventListener paymentEventListener, OrderService orderService) {
        this.paymentEventListener = paymentEventListener;
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all messages trapped in Dead Letter Topic (orders.v1.DLT)")
    public ResponseEntity<List<Map<String, Object>>> getDeadLetterMessages() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ConsumerRecord<String, Object> record : paymentEventListener.getDeadLetterRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("key", record.key());
            map.put("topic", record.topic());
            map.put("partition", record.partition());
            map.put("offset", record.offset());
            map.put("timestamp", record.timestamp());
            map.put("payload", record.value());
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/replay/{orderId}")
    @Operation(summary = "Replay a failed message back to original orders.v1 topic")
    public ResponseEntity<Map<String, Object>> replayOrder(@PathVariable String orderId) {
        boolean success = orderService.replayOrder(orderId);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "REPLAYED",
                    "orderId", orderId,
                    "targetTopic", "orders.v1",
                    "message", "Order successfully re-published to Kafka orders.v1 for reprocessing"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "orderId", orderId,
                    "message", "Order not found or cannot be replayed"
            ));
        }
    }
}
