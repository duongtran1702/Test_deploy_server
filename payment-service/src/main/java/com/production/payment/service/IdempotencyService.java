package com.production.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ensures consumer idempotency.
 * In production, this can be backed by Redis or an idempotency database table.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final Set<String> processedOrderIds = ConcurrentHashMap.newKeySet();

    public boolean isAlreadyProcessed(String orderId) {
        boolean exists = processedOrderIds.contains(orderId);
        if (exists) {
            log.warn("[Idempotency] Duplicate message detected for OrderId: {}. Skipping execution.", orderId);
        }
        return exists;
    }

    public void markAsProcessed(String orderId) {
        processedOrderIds.add(orderId);
    }

    public void clear() {
        processedOrderIds.clear();
    }
}
