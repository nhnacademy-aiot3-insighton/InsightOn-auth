package com.nhnacademy.insightonauth.redis;

import com.nhnacademy.insightonauth.exception.external.RedisOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);   // GETDEL, 원자적
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        // 혹시 null이면 NPE가 날 수 있어서 boolean 비교해서 null의 경우 false가 되게
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
        );
    }

    public long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);   // 원자적 증가

        if (count == null) {
            throw new RedisOperationException("Redis 카운터 증가에 실패했습니다: " + key);
        }

        if (count == 1) {
            redisTemplate.expire(key, ttl);
        }

        return count;
    }
}
