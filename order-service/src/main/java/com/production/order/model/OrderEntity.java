package com.production.order.model;

import com.production.common.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderEntity {
    private String id;
    private String customerId;
    private String item;
    private BigDecimal amount;
    private OrderStatus status;
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;
    private String note;
    private String lastEventError;

    public OrderEntity() {
    }

    public OrderEntity(String id, String customerId, String item, BigDecimal amount,
                       OrderStatus status, String correlationId) {
        this.id = id;
        this.customerId = customerId;
        this.item = item;
        this.amount = amount;
        this.status = status;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getLastEventError() { return lastEventError; }
    public void setLastEventError(String lastEventError) { this.lastEventError = lastEventError; }
}
