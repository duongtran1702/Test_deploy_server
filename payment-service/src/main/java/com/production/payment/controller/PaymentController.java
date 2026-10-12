package com.production.payment.controller;

import com.production.common.dto.OrderEvent;
import com.production.common.dto.PaymentResultEvent;
import com.production.common.model.OrderStatus;
import com.production.payment.model.PaymentTransaction;
import com.production.payment.service.ChaosService;
import com.production.payment.service.PaymentProcessorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment Controller", description = "Endpoints for direct payment processing and transaction ledger")
public class PaymentController {

    private final PaymentProcessorService paymentProcessorService;
    private final ChaosService chaosService;

    public PaymentController(PaymentProcessorService paymentProcessorService, ChaosService chaosService) {
        this.paymentProcessorService = paymentProcessorService;
        this.chaosService = chaosService;
    }

    @PostMapping("/process-direct")
    @Operation(summary = "Direct synchronous payment processing",
               description = "Direct REST call used to test Resilience4j Circuit Breaker, Retries, and Fallback.")
    public ResponseEntity<PaymentResultEvent> processDirectPayment(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        String customerId = (String) body.getOrDefault("customerId", "Direct-Customer");
        Object amountObj = body.get("amount");
        BigDecimal amount = amountObj != null ? new BigDecimal(amountObj.toString()) : BigDecimal.valueOf(100.0);

        OrderEvent syntheticEvent = new OrderEvent(
                orderId,
                customerId,
                "Direct API Call",
                amount,
                OrderStatus.PROCESSING,
                "DIRECT-REST-" + System.currentTimeMillis(),
                Instant.now(),
                false,
                false
        );

        PaymentResultEvent result = paymentProcessorService.processOrderPayment(syntheticEvent, 1);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all processed payment transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactions() {
        return ResponseEntity.ok(paymentProcessorService.getAllTransactions());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get payment transaction metrics and chaos status")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(paymentProcessorService.getMetrics());
    }

    @DeleteMapping("/transactions")
    @Operation(summary = "Clear payment transactions and idempotency cache")
    public ResponseEntity<Map<String, String>> clearTransactions() {
        paymentProcessorService.clear();
        return ResponseEntity.ok(Map.of("message", "Payment transactions and idempotency cache cleared"));
    }
}
