package com.production.payment.service;

import com.production.common.dto.OrderEvent;
import com.production.common.model.OrderStatus;
import com.production.payment.model.ChaosConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChaosServiceTest {

    private ChaosService chaosService;

    @BeforeEach
    void setUp() {
        chaosService = new ChaosService();
    }

    @Test
    void testPoisonPillTriggersUnrecoverableException() {
        OrderEvent poisonEvent = new OrderEvent(
                "ORD-POISON", "Hacker", "Poison Bomb",
                BigDecimal.TEN, OrderStatus.PROCESSING, "CORR-1",
                Instant.now(), true, false
        );

        assertThrows(IllegalArgumentException.class, () -> chaosService.applyChaos(poisonEvent));
    }

    @Test
    void testGatewayDownSimulation() {
        ChaosConfig config = new ChaosConfig();
        config.setSimulateGatewayDown(true);
        chaosService.updateConfig(config);

        OrderEvent normalEvent = new OrderEvent(
                "ORD-NORMAL", "Alice", "Book",
                BigDecimal.TEN, OrderStatus.PROCESSING, "CORR-2",
                Instant.now(), false, false
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chaosService.applyChaos(normalEvent));
        assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    void testNormalFlowDoesNotThrow() {
        OrderEvent normalEvent = new OrderEvent(
                "ORD-NORMAL", "Alice", "Book",
                BigDecimal.TEN, OrderStatus.PROCESSING, "CORR-3",
                Instant.now(), false, false
        );

        assertDoesNotThrow(() -> chaosService.applyChaos(normalEvent));
    }
}
