package com.ticket.ddd.application.service.ticket.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticket.ddd.application.model.cache.TicketDetailCache;
import com.ticket.ddd.domain.model.entity.TicketDetail;
import com.ticket.ddd.domain.service.TicketDetailDomainService;
import com.ticket.ddd.infrastructure.cache.redis.RedisInfraService;
import com.ticket.ddd.infrastructure.distributed.redisson.impl.RedisDistributedLocker;
import com.ticket.ddd.infrastructure.distributed.redisson.impl.RedisDistributedService;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class TicketDetailCacheService {
    @Autowired
    private RedisDistributedService redisDistributedService;
    @Autowired
    private RedisInfraService redisInfraService;
    @Autowired
    private TicketDetailDomainService ticketDetailDomainService;

    private final static Cache<Long,TicketDetailCache> ticketDetailLocalCache = CacheBuilder.newBuilder()
    .initialCapacity(10)
    .concurrencyLevel(12)
    .expireAfterWrite(5,TimeUnit.MINUTES)
    .build();

    public boolean orderTicketByUser(Long ticketId) {
        ticketDetailLocalCache.invalidate(ticketId);
        redisInfraService.delete(genEventItemKey(ticketId));
        return true;
    }

    public TicketDetailCache getTicketDetail(Long ticketId,Long version) {
        TicketDetailCache ticketDetailCache = getTicketDetailLocalCache(ticketId);
        if (ticketDetailCache != null) {
            if (version == null) {
                log.info("01:Get Ticket From Local Cache: versionUser{}, versionLocal{}",version,ticketDetailCache.getVersion());

                return ticketDetailCache;
            }
            if(version.equals(ticketDetailCache.getVersion())){
                log.info("02:Get Ticket From Local Cache: versionUser{}, versionLocal{}",version,ticketDetailCache.getVersion());
                return ticketDetailCache;
            }
            if(version < ticketDetailCache.getVersion()) {
                log.info("03:Get Ticket From Local Cache: versionUser{}, versionLocal{}",version,ticketDetailCache.getVersion());
                return ticketDetailCache;
            }
            if(version > ticketDetailCache.getVersion()) {
                log.info("04:Get Ticket From Distributed Cache: versionUser{}, versionLocal{}",version,ticketDetailCache.getVersion());
                return getTicketDetailDistributedCache(ticketId);
            }
        }
        return getTicketDetailDistributedCache(ticketId);

    }
    public TicketDetailCache getTicketDetailLocalCache(Long ticketId) {
        return ticketDetailLocalCache.getIfPresent(ticketId);
    }
    public TicketDetailCache getTicketDetailDatabase(Long ticketId) {
        RedisDistributedLocker locker = redisDistributedService.getDistributedLock(genEventItemKey(ticketId));
        try {
            boolean isLock = locker.tryLock(1L,5L,TimeUnit.SECONDS);
            if(!isLock) {
                return null;
            }

            TicketDetailCache ticketDetailCache = redisInfraService.getObject(genEventItemKey(ticketId), TicketDetailCache.class);
            if(ticketDetailCache != null) {
                return ticketDetailCache;
            }
            TicketDetail ticketDetail = ticketDetailDomainService.getTicketDetailById(ticketId);
            ticketDetailCache = new TicketDetailCache().withClone(ticketDetail).withVersion(System.currentTimeMillis());
            redisInfraService.setObject(genEventItemKey(ticketId), ticketDetailCache);
            return ticketDetailCache;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }
    }
    public TicketDetailCache getTicketDetailDistributedCache(Long ticketId) {
        TicketDetailCache ticketDetailCache = redisInfraService.getObject(genEventItemKey(ticketId), TicketDetailCache.class);
        if (ticketDetailCache == null) {
            log.info("Get Ticketfrom distributed lock");
            ticketDetailCache = getTicketDetailDatabase(ticketId);
        }

        ticketDetailLocalCache.put(ticketId, ticketDetailCache);
        log.info("Get Ticket From Distributed Cache");
        return ticketDetailCache;
    }
    private String genEventItemKey(Long ticketId) {
        return "PRO_TICKET:ITEM" + ticketId;
    }

}
