package com.production.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
    }

    @Test
    void testIdempotencyDeduplication() {
        String orderId = "ORD-TEST-123";

        // First attempt - not processed yet
        assertFalse(idempotencyService.isAlreadyProcessed(orderId));

        // Mark as processed
        idempotencyService.markAsProcessed(orderId);

        // Second attempt - must detect duplicate
        assertTrue(idempotencyService.isAlreadyProcessed(orderId));
    }

    @Test
    void testClear() {
        String orderId = "ORD-TEST-456";
        idempotencyService.markAsProcessed(orderId);
        assertTrue(idempotencyService.isAlreadyProcessed(orderId));

        idempotencyService.clear();
        assertFalse(idempotencyService.isAlreadyProcessed(orderId));
    }
}
