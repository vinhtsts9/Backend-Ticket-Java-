package com.ticket.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.PreDestroy;

/**
 * Graceful Shutdown Handler
 * Ensures proper cleanup when application shuts down
 */
@Configuration
@Slf4j
public class GracefulShutdownConfig {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @PreDestroy
    public void gracefulShutdown() {
        log.info("================== GRACEFUL SHUTDOWN STARTED ==================");
        long startTime = System.currentTimeMillis();

        try {
            // Close Redis connection
            log.info("[SHUTDOWN] Closing Redis connection...");
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                try {
                    redisTemplate.getConnectionFactory().getConnection().close();
                    log.info("[SHUTDOWN] Redis connection closed");
                } catch (Exception e) {
                    log.warn("[SHUTDOWN] Error closing Redis connection: {}", e.getMessage());
                }
            }

            // Wait for in-flight requests
            log.info("[SHUTDOWN] Waiting 5s for in-flight requests...");
            Thread.sleep(5000);

            long duration = System.currentTimeMillis() - startTime;
            log.info("================== GRACEFUL SHUTDOWN COMPLETED in {} ms ==================", duration);

        } catch (Exception e) {
            log.error("[SHUTDOWN] Error during graceful shutdown", e);
        }
    }
}

