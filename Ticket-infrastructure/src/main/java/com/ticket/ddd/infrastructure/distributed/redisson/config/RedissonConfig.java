package com.ticket.ddd.infrastructure.distributed.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            config.useSentinelServers()
                    .setMasterName("mymaster")
                    .addSentinelAddress(
                            "redis://redis-sentinel1:26379",
                            "redis://redis-sentinel2:26379",
                            "redis://redis-sentinel3:26379"
                    )
                    .setPassword("123456")
                    .setCheckSentinelsList(false)
                    .setDatabase(0);

            return Redisson.create(config);
        } catch (Exception e) {
            logger.error("Không thể kết nối Redis Sentinel. Tiếp tục chạy mà không có Redis.", e);
            return null; // hoặc trả về một mock nếu cần
        }
    }

}
