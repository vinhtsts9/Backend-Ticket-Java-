package com.ticket.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health Check Configuration for critical dependencies
 * - Database connectivity
 * - Redis connectivity
 */
@Configuration
@Slf4j
public class HealthCheckConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * Database Health Indicator
     * Checks if database connection is available
     */
    @Bean
    public HealthIndicator dbHealthIndicator() {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(3)) {
                    log.debug("[HEALTH] Database connection is healthy");
                    return Health.up()
                            .withDetail("database", "MySQL 8.0")
                            .withDetail("status", "Available")
                            .build();
                }
            } catch (Exception e) {
                log.warn("[HEALTH] Database health check failed: {}", e.getMessage());
            }
            return Health.down()
                    .withDetail("database", "MySQL 8.0")
                    .withDetail("status", "Unavailable")
                    .build();
        };
    }

    /**
     * Redis Health Indicator
     * Checks if Redis/Redis Sentinel is available
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                if (redisConnectionFactory == null) {
                    log.warn("[HEALTH] RedisConnectionFactory not available - Redis not configured");
                    return Health.down()
                            .withDetail("redis", "Redis Sentinel Cluster")
                            .withDetail("status", "Not configured")
                            .build();
                }
                
                redisConnectionFactory.getConnection().ping();
                log.debug("[HEALTH] Redis connection is healthy");
                return Health.up()
                        .withDetail("redis", "Redis Sentinel Cluster")
                        .withDetail("status", "Available")
                        .build();
            } catch (Exception e) {
                log.warn("[HEALTH] Redis health check failed: {}", e.getMessage());
            }
            return Health.down()
                    .withDetail("redis", "Redis Sentinel Cluster")
                    .withDetail("status", "Unavailable")
                    .build();
        };
    }

    /**
     * Connection Pool Health Indicator
     * Monitors HikariCP pool status
     */
    @Bean
    public HealthIndicator connectionPoolHealthIndicator() {
        return () -> {
            try {
                if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikariDs) {
                    int activeConnections = hikariDs.getHikariPoolMXBean().getActiveConnections();
                    int idleConnections = hikariDs.getHikariPoolMXBean().getIdleConnections();
                    int totalConnections = activeConnections + idleConnections;
                    
                    log.debug("[HEALTH] Pool status - Active: {}, Idle: {}, Total: {}", 
                        activeConnections, idleConnections, totalConnections);
                    
                    return Health.up()
                            .withDetail("poolName", "HikariCP")
                            .withDetail("activeConnections", activeConnections)
                            .withDetail("idleConnections", idleConnections)
                            .withDetail("totalConnections", totalConnections)
                            .build();
                }
            } catch (Exception e) {
                log.warn("[HEALTH] Connection pool health check failed: {}", e.getMessage());
            }
            return Health.down()
                    .withDetail("poolName", "HikariCP")
                    .withDetail("status", "Unable to retrieve pool status")
                    .build();
        };
    }
}
