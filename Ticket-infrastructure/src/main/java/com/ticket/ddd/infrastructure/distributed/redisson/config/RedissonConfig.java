package com.ticket.ddd.infrastructure.distributed.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Value("${spring.redis.sentinel.master:mymaster}")
    private String sentinelMaster;

    @Value("${spring.redis.sentinel.nodes:redis-sentinel1:26379,redis-sentinel2:26379,redis-sentinel3:26379}")
    private String sentinelNodes;

    @Value("${spring.redis.password:123456}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redissonClient() {
        logger.info("Khởi tạo RedissonClient với Sentinel...");
        logger.info("Master: {}, Nodes: {}", sentinelMaster, sentinelNodes);
        
        try {
            Config config = new Config();
            var sentinelServersConfig = config.useSentinelServers()
                    .setMasterName(sentinelMaster)
                    .setPassword(redisPassword)
                    .setCheckSentinelsList(false)
                    .setDatabase(0);
            
            // Parse sentinel nodes (format: "host1:port1,host2:port2,...")
            String[] nodes = sentinelNodes.split(",");
            for (String node : nodes) {
                String[] parts = node.trim().split(":");
                if (parts.length == 2) {
                    String address = "redis://" + parts[0] + ":" + parts[1];
                    sentinelServersConfig.addSentinelAddress(address);
                    logger.info("✅ Thêm Sentinel: {} : {}", parts[0], parts[1]);
                }
            }

            RedissonClient client = Redisson.create(config);
            logger.info("✅ RedissonClient đã kết nối thành công với Sentinel!");
            return client;
        } catch (Exception e) {
            logger.error("❌ Lỗi kết nối Redis Sentinel: {}", e.getMessage());
            logger.error("Stack trace:", e);
            // Return a dummy client instead of throwing - let app start without Redis
            logger.warn("⚠️ Ứng dụng sẽ hoạt động mà không có Redisson");
            return null;
        }
    }

}
