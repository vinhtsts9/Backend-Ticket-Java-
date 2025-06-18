package com.ticket.ddd.infrastructure.distributed.redisson.impl;

import org.springframework.stereotype.Service;

@Service
public interface RedisDistributedService {
    RedisDistributedLocker getDistributedLock(String lockKey);
}
