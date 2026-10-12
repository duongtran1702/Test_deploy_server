package com.production.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OrderRequest {

    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;

    @NotBlank(message = "Item name must not be blank")
    private String item;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly positive")
    private BigDecimal amount;

    private boolean simulateFailure = false;
    private boolean simulatePoisonPill = false;

    public OrderRequest() {
    }

    public OrderRequest(String customerId, String item, BigDecimal amount,
                        boolean simulateFailure, boolean simulatePoisonPill) {
        this.customerId = customerId;
        this.item = item;
        this.amount = amount;
        this.simulateFailure = simulateFailure;
        this.simulatePoisonPill = simulatePoisonPill;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public boolean isSimulateFailure() { return simulateFailure; }
    public void setSimulateFailure(boolean simulateFailure) { this.simulateFailure = simulateFailure; }

    public boolean isSimulatePoisonPill() { return simulatePoisonPill; }
    public void setSimulatePoisonPill(boolean simulatePoisonPill) { this.simulatePoisonPill = simulatePoisonPill; }
}
