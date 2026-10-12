package com.production.common.dto;

import com.production.common.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResultEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String reason;
    private String correlationId;
    private Instant timestamp;
    private int retryCount;
    private String processedByService;

    public PaymentResultEvent() {
    }

    public PaymentResultEvent(String paymentId, String orderId, String customerId,
                              BigDecimal amount, PaymentStatus status, String reason,
                              String correlationId, Instant timestamp, int retryCount,
                              String processedByService) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.retryCount = retryCount;
        this.processedByService = processedByService;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getProcessedByService() { return processedByService; }
    public void setProcessedByService(String processedByService) { this.processedByService = processedByService; }
}
