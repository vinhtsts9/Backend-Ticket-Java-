package com.ticket.ddd.application.service.order.cache;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.ticket.ddd.application.model.cache.TicketDetailCache;
import com.ticket.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.ticket.ddd.infrastructure.cache.redis.RedisInfraService;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;



@Service
@Slf4j
public class StockOrderCacheService {
    @Autowired
    private TicketDetailCacheService ticketDetailCacheService;

    @Autowired
    private RedisInfraService redisInfraService;
    @PostConstruct
    public void logRedisConnectionInfo() {
        var factory = redisInfraService.getRedisTemplate().getConnectionFactory();
        if (factory instanceof LettuceConnectionFactory lettuce) {
            String host = lettuce.getHostName();
            int port = lettuce.getPort();
            log.info("[REDIS-CONN] App connected to Redis at {}:{}", host, port);
        } else {
            log.warn("[REDIS-CONN] Không xác định được LettuceConnectionFactory");
        }
    }
    public boolean AddStockAvailableToCache(Long ticketId) {
        if(ticketId == null) {
            return false;
        }
        TicketDetailCache ticketDetailCache = ticketDetailCacheService.getTicketDetail(ticketId, null);
        if(ticketDetailCache == null) {
            return false;
        }
        String keyStockItemCache = getKeyStockItemCache(ticketId);
        redisInfraService.setInt(keyStockItemCache, ticketDetailCache.getTicketDetail().getStockAvailable());
        return true;
    }

    public int decreaseStockCache(Long ticketId, Integer quantity) {
        String keyStockNormal = getKeyStockItemCache(ticketId);
        int StockAvailable = redisInfraService.getInt(keyStockNormal);
        log.info("stockAvailable Normal: {}, {}, {} ", keyStockNormal, StockAvailable, String.valueOf(StockAvailable - quantity));
        if(StockAvailable > quantity) {
            redisInfraService.setInt(keyStockNormal, StockAvailable- quantity);
            log.info("stockAvailable racing...: {}", StockAvailable - quantity);
            return 1;
        }
        return 0;
    }
    public int decreaseStockCacheByLua(Long ticketId,Integer quantity) {
        String keyStockLUA = getKeyStockItemCache(ticketId);
        log.info("[LUA] Enter decreaseStockCacheByLua - ticketId: {}, quantity: {}, key: {}", ticketId, quantity, keyStockLUA);

        String luaScript = "local stock = tonumber(redis.call('GET',KEYS[1]))" +
                "if (stock >= tonumber(ARGV[1])) then " +
                "redis.call('SET', KEYS[1],stock -tonumber(ARGV[1])); " +
                "return 1;" +
                "end; " +
                "return 0; ";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript,Long.class);
        Long result = redisInfraService.getRedisTemplate().execute(redisScript,Collections.singletonList(keyStockLUA),quantity);

        if (result == null) {
            log.warn("[LUA] Redis script returned null for ticketId {}", ticketId);
            return 0;
        }

        log.info("[LUA] Script execution result: {}", result);

        Integer after = redisInfraService.getInt(keyStockLUA);
        log.info("[LUA] Redis stock after Lua: {}", after);
        return result.intValue();
    }
    private String getKeyStockItemCache(Long ticketId) {
        return "TICKET:"+ ticketId + ":STOCK";
    }

    private String getKeyStockCacheLUA(Long ticketId){
        return "LUA:TICKET:" + ticketId + ":STOCK";
    }
}
