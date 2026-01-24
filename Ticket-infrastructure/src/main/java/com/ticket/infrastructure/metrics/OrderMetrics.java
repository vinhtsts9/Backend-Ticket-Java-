package com.ticket.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 * Custom Metrics for Order Processing
 * Tracks:
 * - Stock depletion rate
 * - Order success/failure rate
 * - Processing time
 * - Pending orders
 */
@Component
@Slf4j
public class OrderMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger pendingOrders;
    private final AtomicInteger failedCompensations;

    @Autowired
    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.pendingOrders = new AtomicInteger(0);
        this.failedCompensations = new AtomicInteger(0);

        // Register custom gauges
        Gauge.builder("ticket.orders.pending", pendingOrders, AtomicInteger::get)
                .description("Number of pending order requests")
                .tag("service", "ticket-order")
                .register(meterRegistry);

        Gauge.builder("ticket.compensations.failed", failedCompensations, AtomicInteger::get)
                .description("Number of failed Redis compensations")
                .tag("service", "ticket-order")
                .register(meterRegistry);

        log.info("[METRICS] OrderMetrics initialized");
    }

    /**
     * Record order attempt
     * 
     * @param ticketId The ticket ID
     * @param quantity The quantity ordered
     * @param success Whether the order succeeded
     * @param durationMs Processing time in milliseconds
     */
    public void recordOrderAttempt(Long ticketId, int quantity, boolean success, long durationMs) {
        try {
            // Timer metric
            meterRegistry.timer("ticket.order.process.duration",
                    "ticketId", String.valueOf(ticketId),
                    "success", String.valueOf(success))
                    .record(durationMs, TimeUnit.MILLISECONDS);

            // Counter metric
            meterRegistry.counter("ticket.order.attempt",
                    "ticketId", String.valueOf(ticketId),
                    "success", String.valueOf(success))
                    .increment();

            // Quantity metric
            meterRegistry.counter("ticket.order.quantity",
                    "ticketId", String.valueOf(ticketId),
                    "success", String.valueOf(success))
                    .increment(quantity);

            if (success) {
                log.debug("[METRICS] Order successful: ticketId={}, quantity={}, duration={}ms", 
                    ticketId, quantity, durationMs);
            } else {
                log.debug("[METRICS] Order failed: ticketId={}, quantity={}, duration={}ms", 
                    ticketId, quantity, durationMs);
            }
        } catch (Exception e) {
            log.warn("[METRICS] Failed to record order attempt: {}", e.getMessage());
        }
    }

    /**
     * Record stock depletion
     * 
     * @param ticketId The ticket ID
     * @param remainingStock The remaining stock after order
     */
    public void recordStockDepletion(Long ticketId, int remainingStock) {
        try {
            io.micrometer.core.instrument.Gauge.builder("ticket.stock.remaining", 
                    () -> remainingStock)
                    .tag("ticketId", String.valueOf(ticketId))
                    .register(meterRegistry);

            log.debug("[METRICS] Stock depleted: ticketId={}, remaining={}", ticketId, remainingStock);
        } catch (Exception e) {
            log.warn("[METRICS] Failed to record stock depletion: {}", e.getMessage());
        }
    }

    /**
     * Record Redis compensation (rollback)
     * 
     * @param ticketId The ticket ID
     * @param quantity The quantity rolled back
     * @param success Whether the compensation succeeded
     */
    public void recordCompensation(Long ticketId, int quantity, boolean success) {
        try {
            meterRegistry.counter("ticket.redis.compensation",
                    "ticketId", String.valueOf(ticketId),
                    "success", String.valueOf(success))
                    .increment();

            if (!success) {
                failedCompensations.incrementAndGet();
            }

            if (success) {
                log.debug("[METRICS] Compensation successful: ticketId={}, quantity={}", ticketId, quantity);
            } else {
                log.warn("[METRICS] Compensation failed: ticketId={}, quantity={}", ticketId, quantity);
            }
        } catch (Exception e) {
            log.warn("[METRICS] Failed to record compensation: {}", e.getMessage());
        }
    }

    /**
     * Increment pending orders
     */
    public void incrementPendingOrders() {
        pendingOrders.incrementAndGet();
        log.debug("[METRICS] Pending orders: {}", pendingOrders.get());
    }

    /**
     * Decrement pending orders
     */
    public void decrementPendingOrders() {
        int count = pendingOrders.decrementAndGet();
        if (count < 0) {
            pendingOrders.set(0);
        }
        log.debug("[METRICS] Pending orders: {}", pendingOrders.get());
    }

    /**
     * Get current pending orders count
     */
    public int getPendingOrdersCount() {
        return pendingOrders.get();
    }

    /**
     * Get failed compensations count
     */
    public int getFailedCompensationsCount() {
        return failedCompensations.get();
    }

    /**
     * Reset metrics (for testing purposes)
     */
    public void reset() {
        pendingOrders.set(0);
        failedCompensations.set(0);
        log.info("[METRICS] All metrics reset");
    }
}
