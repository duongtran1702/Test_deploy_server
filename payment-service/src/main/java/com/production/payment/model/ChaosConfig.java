package com.production.payment.model;

public class ChaosConfig {
    private int failureRatePercent = 0;
    private long simulatedLatencyMs = 0;
    private boolean simulateGatewayDown = false;
    private boolean forceDLTRouting = false;

    public ChaosConfig() {
    }

    public ChaosConfig(int failureRatePercent, long simulatedLatencyMs, boolean simulateGatewayDown, boolean forceDLTRouting) {
        this.failureRatePercent = failureRatePercent;
        this.simulatedLatencyMs = simulatedLatencyMs;
        this.simulateGatewayDown = simulateGatewayDown;
        this.forceDLTRouting = forceDLTRouting;
    }

    public int getFailureRatePercent() { return failureRatePercent; }
    public void setFailureRatePercent(int failureRatePercent) { this.failureRatePercent = failureRatePercent; }

    public long getSimulatedLatencyMs() { return simulatedLatencyMs; }
    public void setSimulatedLatencyMs(long simulatedLatencyMs) { this.simulatedLatencyMs = simulatedLatencyMs; }

    public boolean isSimulateGatewayDown() { return simulateGatewayDown; }
    public void setSimulateGatewayDown(boolean simulateGatewayDown) { this.simulateGatewayDown = simulateGatewayDown; }

    public boolean isForceDLTRouting() { return forceDLTRouting; }
    public void setForceDLTRouting(boolean forceDLTRouting) { this.forceDLTRouting = forceDLTRouting; }
}
