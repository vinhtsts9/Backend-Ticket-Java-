package com.ticket.ddd.infrastructure.cache.redis;

import org.springframework.data.redis.core.RedisTemplate;

public interface RedisInfraService {
    void setString(String key,String value);
    String getString(String key);
    void setObject(String key,Object value);
    <T> T getObject(String key,Class<T> targetClass);
    void delete(String key);
    void setInt(String key,int value);
    int getInt(String key);
    RedisTemplate<String,Object> getRedisTemplate();
}
