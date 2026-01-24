package com.ticket.ddd.infrastructure.cache.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Compensating Transaction Service for Redis Cache
 * Handles rollback of cache operations when database operations fail
 */
@Service
@Slf4j
public class RedisCompensatingTransaction {

    @Autowired
    private RedisInfraService redisInfraService;

    private static final String STOCK_CACHE_PREFIX = "ticket:stock:";

    /**
     * Increase stock cache (used for rollback)
     * 
     * @param ticketId The ticket ID
     * @param quantity The quantity to increase
     * @return true if successful, false otherwise
     */
    public boolean increaseStockCache(Long ticketId, Integer quantity) {
        if (ticketId == null || quantity == null || quantity <= 0) {
            log.warn("[COMPENSATE] Invalid parameters: ticketId={}, quantity={}", ticketId, quantity);
            return false;
        }

        String keyStock = getKeyStockCache(ticketId);
        
        try {
            // Use Lua script to increase stock atomically
            String luaScript = "local stock = tonumber(redis.call('GET', KEYS[1])) or 0; " +
                    "redis.call('SET', KEYS[1], stock + tonumber(ARGV[1])); " +
                    "return stock + tonumber(ARGV[1]);";
            
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript = 
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
            
            Long result = redisInfraService.getRedisTemplate().execute(
                redisScript, 
                Collections.singletonList(keyStock), 
                quantity
            );

            if (result != null) {
                log.info("[COMPENSATE] Stock increased for ticketId={}: {} + {} = {}", 
                    ticketId, result - quantity, quantity, result);
                return true;
            } else {
                log.error("[COMPENSATE] Redis script returned null for ticketId={}", ticketId);
                return false;
            }
        } catch (Exception e) {
            log.error("[COMPENSATE] Failed to increase stock cache for ticketId={}, quantity={}", 
                ticketId, quantity, e);
            return false;
        }
    }

    /**
     * Delete stock cache entry (nuclear option for rollback)
     * 
     * @param ticketId The ticket ID
     * @return true if successful
     */
    public boolean deleteStockCache(Long ticketId) {
        if (ticketId == null) {
            log.warn("[COMPENSATE] ticketId is null");
            return false;
        }

        try {
            String keyStock = getKeyStockCache(ticketId);
            Boolean result = redisInfraService.getRedisTemplate().delete(keyStock);
            
            if (result != null && result) {
                log.info("[COMPENSATE] Stock cache deleted for ticketId={}", ticketId);
            }
            return true;
        } catch (Exception e) {
            log.error("[COMPENSATE] Failed to delete stock cache for ticketId={}", ticketId, e);
            return false;
        }
    }

    /**
     * Rollback transaction operation log
     * Records compensation attempts for monitoring
     * 
     * @param ticketId The ticket ID
     * @param operation The operation that failed
     * @param reason The reason for compensation
     */
    public void logCompensation(Long ticketId, String operation, String reason) {
        try {
            String logKey = "compensation:log:" + System.currentTimeMillis();
            String logValue = String.format("ticketId=%d, operation=%s, reason=%s", 
                ticketId, operation, reason);
            
            redisInfraService.getRedisTemplate()
                .opsForValue()
                .set(logKey, logValue, java.time.Duration.ofHours(24));
            
            log.info("[COMPENSATE-LOG] {}", logValue);
        } catch (Exception e) {
            log.error("[COMPENSATE-LOG] Failed to log compensation for ticketId={}", ticketId, e);
        }
    }

    /**
     * Get Redis key for stock cache
     */
    private String getKeyStockCache(Long ticketId) {
        return STOCK_CACHE_PREFIX + ticketId;
    }

    /**
     * Get current stock from cache (for verification)
     * 
     * @param ticketId The ticket ID
     * @return Current stock, or -1 if not found
     */
    public int getStockFromCache(Long ticketId) {
        if (ticketId == null) {
            return -1;
        }

        try {
            String keyStock = getKeyStockCache(ticketId);
            Object value = redisInfraService.getRedisTemplate()
                .opsForValue()
                .get(keyStock);
            
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
            return -1;
        } catch (Exception e) {
            log.error("[COMPENSATE] Failed to get stock from cache for ticketId={}", ticketId, e);
            return -1;
        }
    }
}
