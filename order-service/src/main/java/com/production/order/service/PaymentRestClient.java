package com.production.order.service;

import com.production.common.dto.PaymentResultEvent;
import com.production.common.model.PaymentStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentRestClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentRestClient.class);
    private static final String SERVICE_NAME = "paymentService";

    private final RestClient restClient;

    @Value("${payment.service.url:http://localhost:8082}")
    private String paymentServiceUrl;

    public PaymentRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "paymentFallback")
    @Retry(name = SERVICE_NAME)
    @RateLimiter(name = SERVICE_NAME)
    public Map<String, Object> executeDirectPayment(String orderId, BigDecimal amount, String customerId) {
        log.info("[SyncCall] Calling payment-service synchronously for order: {}", orderId);

        Map<String, Object> request = Map.of(
                "orderId", orderId,
                "amount", amount,
                "customerId", customerId
        );

        return restClient.post()
                .uri(paymentServiceUrl + "/api/payment/process-direct")
                .body(request)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Fallback method executed when Circuit Breaker is OPEN, Rate Limiter is saturated,
     * or retries are exhausted.
     */
    public Map<String, Object> paymentFallback(String orderId, BigDecimal amount, String customerId, Throwable ex) {
        log.warn("[CircuitBreakerFallback] Fallback triggered for order: {}, reason: {}", orderId, ex.getMessage());

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("orderId", orderId);
        fallbackResponse.put("status", "FALLBACK_ACCEPTED");
        fallbackResponse.put("message", "Payment service degraded or unavailable. Queued for asynchronous reconciliation.");
        fallbackResponse.put("fallbackActive", true);
        fallbackResponse.put("errorType", ex.getClass().getSimpleName());
        fallbackResponse.put("errorMessage", ex.getMessage());
        fallbackResponse.put("timestamp", Instant.now().toString());
        return fallbackResponse;
    }
}
