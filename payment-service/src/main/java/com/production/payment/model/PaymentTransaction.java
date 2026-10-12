package com.production.payment.model;

import com.production.common.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentTransaction {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String reason;
    private Instant createdAt;
    private int retryCount;
    private String traceId;

    public PaymentTransaction() {
    }

    public PaymentTransaction(String paymentId, String orderId, String customerId,
                              BigDecimal amount, PaymentStatus status, String reason,
                              int retryCount, String traceId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.retryCount = retryCount;
        this.traceId = traceId;
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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
