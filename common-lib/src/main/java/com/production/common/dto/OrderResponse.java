package com.production.common.dto;

import com.production.common.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse {
    private String orderId;
    private String customerId;
    private String item;
    private BigDecimal amount;
    private OrderStatus status;
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;
    private String note;

    public OrderResponse() {
    }

    public OrderResponse(String orderId, String customerId, String item, BigDecimal amount,
                         OrderStatus status, String correlationId, Instant createdAt,
                         Instant updatedAt, String note) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.item = item;
        this.amount = amount;
        this.status = status;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.note = note;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
