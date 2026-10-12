package com.production.payment.service;

import com.production.common.dto.OrderEvent;
import com.production.payment.model.ChaosConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChaosService {

    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);
    private final ChaosConfig config = new ChaosConfig();

    public ChaosConfig getConfig() {
        return config;
    }

    public void updateConfig(ChaosConfig newConfig) {
        this.config.setFailureRatePercent(newConfig.getFailureRatePercent());
        this.config.setSimulatedLatencyMs(newConfig.getSimulatedLatencyMs());
        this.config.setSimulateGatewayDown(newConfig.isSimulateGatewayDown());
        this.config.setForceDLTRouting(newConfig.isForceDLTRouting());
        log.info("[ChaosService] Configuration updated: FailureRate={}%, Latency={}ms, GatewayDown={}, ForceDLT={}",
                config.getFailureRatePercent(), config.getSimulatedLatencyMs(),
                config.isSimulateGatewayDown(), config.isForceDLTRouting());
    }

    public void applyChaos(OrderEvent event) {
        // 1. Check for simulated latency
        if (config.getSimulatedLatencyMs() > 0) {
            try {
                log.info("[Chaos] Injecting latency: {}ms for order: {}", config.getSimulatedLatencyMs(), event.getOrderId());
                Thread.sleep(config.getSimulatedLatencyMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 2. Check for simulated poison pill or forced DLT
        if (event.isSimulatePoisonPill() || config.isForceDLTRouting()) {
            log.error("[Chaos] FATAL POISON PILL detected in payload for OrderId: {}. Triggering unrecoverable exception for DLT routing!",
                    event.getOrderId());
            throw new IllegalArgumentException("FATAL: Poison pill payload cannot be parsed. Immediate routing to DLT: " + event.getOrderId());
        }

        // 3. Check for downstream gateway outage (503 / network timeout)
        if (config.isSimulateGatewayDown()) {
            log.warn("[Chaos] Simulating payment gateway outage (503 Service Unavailable) for OrderId: {}", event.getOrderId());
            throw new RuntimeException("Payment Gateway 503 Outage: Downstream banking partner connection failed");
        }

        // 4. Check for order-specific failure flag
        if (event.isSimulateFailure()) {
            log.warn("[Chaos] Order flag 'simulateFailure=true' detected for OrderId: {}. Simulating payment decline/retry.", event.getOrderId());
            throw new IllegalStateException("Simulated processing failure requested by client for order: " + event.getOrderId());
        }

        // 5. Random failure rate
        if (config.getFailureRatePercent() > 0) {
            int roll = ThreadLocalRandom.current().nextInt(100);
            if (roll < config.getFailureRatePercent()) {
                log.warn("[Chaos] Chaos failure roll matched ({} < {}%) for OrderId: {}",
                        roll, config.getFailureRatePercent(), event.getOrderId());
                throw new RuntimeException("Chaos Engineering fault injected at random rate: " + config.getFailureRatePercent() + "%");
            }
        }
    }
}
