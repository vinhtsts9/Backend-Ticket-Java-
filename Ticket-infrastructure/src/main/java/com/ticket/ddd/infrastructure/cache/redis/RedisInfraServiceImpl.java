package com.ticket.ddd.infrastructure.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class RedisInfraServiceImpl implements RedisInfraService {
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Override
    public void setString(String key,String value) {
        if(!StringUtils.hasLength(key)) {
            return;
        }
        redisTemplate.opsForValue().set(key,value);
    }

    @Override
    public String getString(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(String::valueOf)
                .orElse(null);
    }
    @Override
    public void setObject(String key,Object value) {
        if(!StringUtils.hasLength(key)) {
            return;
        }
        try{
            redisTemplate.opsForValue().set(key,value);
        }catch(Exception e) {}
    }
    @Override
    public <T> T getObject(String key,Class<T> targetClass) {
        Object result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            return null;
        }
        if (result instanceof Map) {
            try{
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.convertValue(result,targetClass);
            }catch(IllegalArgumentException e) {
                return null;
            }
        }
        if (result instanceof  String) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue((String) result,targetClass);
            }catch(JsonProcessingException e) {
                return null;
            }
        }

        return null;
    }
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    @Override
    public void setInt(String key, int value) {
            redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public int getInt(String key) {
        return (int) redisTemplate.opsForValue().get(key);
    }
    @Override
    public RedisTemplate<String,Object> getRedisTemplate() {
        return redisTemplate;
    }
 }
