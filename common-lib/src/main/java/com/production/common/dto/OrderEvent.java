package com.production.common.dto;

import com.production.common.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderEvent {
    private String orderId;
    private String customerId;
    private String item;
    private BigDecimal amount;
    private OrderStatus status;
    private String correlationId;
    private Instant timestamp;
    private boolean simulatePoisonPill;
    private boolean simulateFailure;

    public OrderEvent() {
    }

    public OrderEvent(String orderId, String customerId, String item, BigDecimal amount,
                      OrderStatus status, String correlationId, Instant timestamp,
                      boolean simulatePoisonPill, boolean simulateFailure) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.item = item;
        this.amount = amount;
        this.status = status;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.simulatePoisonPill = simulatePoisonPill;
        this.simulateFailure = simulateFailure;
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

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public boolean isSimulatePoisonPill() { return simulatePoisonPill; }
    public void setSimulatePoisonPill(boolean simulatePoisonPill) { this.simulatePoisonPill = simulatePoisonPill; }

    public boolean isSimulateFailure() { return simulateFailure; }
    public void setSimulateFailure(boolean simulateFailure) { this.simulateFailure = simulateFailure; }
}
