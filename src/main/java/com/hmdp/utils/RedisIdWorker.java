package com.hmdp.utils;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RedisIdWorker {

    private final long BEGIN_TIMESTAMP = 1694502400000L;
    //基于Redis实现全局ID生成器

    private final int COUNT_BIT = 32;
    private final RedisTemplate redisTemplate;

    public RedisIdWorker(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    //生成ID
    public Long nextId(String ketPrifex) {
        LocalDateTime now = LocalDateTime.now();
         long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;

         //生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长序列号
        long count = redisTemplate.opsForValue().increment("icr:" + ketPrifex+date);
        return timestamp<<COUNT_BIT|count;

    }
}
