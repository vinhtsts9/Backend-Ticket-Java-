package com.ticket.ddd.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.sentinel.master:mymaster}")
    private String sentinelMaster;

    @Value("${spring.redis.sentinel.nodes:redis-sentinel1:26379,redis-sentinel2:26379,redis-sentinel3:26379}")
    private String sentinelNodes;

    @Value("${spring.redis.password:123456}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Khởi tạo RedisConnectionFactory với cấu hình Sentinel...");
        logger.info("Master: {}, Nodes: {}", sentinelMaster, sentinelNodes);

        try {
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                    .master(sentinelMaster);
            
            // Parse sentinel nodes (format: "host1:port1,host2:port2,...")
            String[] nodes = sentinelNodes.split(",");
            for (String node : nodes) {
                String[] parts = node.trim().split(":");
                if (parts.length == 2) {
                    sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
                    logger.info("✅ Thêm Sentinel: {} : {}", parts[0], parts[1]);
                }
            }
            
            sentinelConfig.setPassword(RedisPassword.of(redisPassword));

            LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig);
            logger.info("✅ RedisConnectionFactory đã được tạo thành công.");
            return factory;
        } catch (Exception e) {
            logger.error("❌ Lỗi khi khởi tạo Redis Sentinel connection: {}", e.getMessage(), e);
            logger.warn("⚠️ Ứng dụng sẽ hoạt động mà không có Redis Sentinel");
            return null;
        }
    }

    @Bean
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("Bắt đầu khởi tạo RedisTemplate...");

        try {
            if (connectionFactory == null) {
                logger.warn("RedisConnectionFactory is null - skipping RedisTemplate creation");
                return null;
            }

            RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(connectionFactory);

            Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(serializer);
            redisTemplate.setHashKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashValueSerializer(serializer);

            redisTemplate.afterPropertiesSet();

            logger.info("✅ RedisTemplate đã được khởi tạo thành công.");
            return redisTemplate;
        } catch (Exception e) {
            logger.error("❌ Lỗi khi khởi tạo RedisTemplate: {}", e.getMessage(), e);
            logger.warn("⚠️ Ứng dụng sẽ hoạt động mà không có RedisTemplate");
            return null;
        }
    }
}
