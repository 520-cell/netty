package com.su.utile;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class DCS_Lock {
    private StringRedisTemplate redisTemplate;
    private String lua1 = "local key = KEYS[1]\n" +
            "local value1 = ARGV[]1\n" +
            "local value2 = ARGV[2]\n" +
            "local result = redis.call('setnx', key, value1 )\n" +
            "if result == 1 then\n" +
            "redis.call('expire', key, value2)\n" +
            "return '1'\n" +
            "else\n" +
            "return '0'";
    private String lua2 = "local key = KEYS[1]\n" +
            "local value1 = ARGV[1]\n" +
            "local result = redis.call('get', key)\n" +
            "if result == 1 then\n" +
            "redis.call('del', value1)\n" +
            "return '1'\n" +
            "end\n" +
            "return '0'";
    private ThreadLocal<String> threadLocal = new ThreadLocal();
    public Boolean getLock(String key, String time){
        String uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
        String result = redisTemplate.execute(new DefaultRedisScript<>(lua1, String.class), Collections.singletonList(key), uuid, time);
        return Integer.parseInt(result) == 1;
    }
    public Boolean getLock1(String key, String time){
        Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent("key", key, 6, TimeUnit.SECONDS);
        return setIfAbsent;
    }
    public Boolean delLock(String key){
        String uuid = threadLocal.get();
        String result = redisTemplate.execute(new DefaultRedisScript<>(lua2, String.class), Collections.singletonList(key), uuid);
        return Integer.parseInt(result) == 1;
    }
}
