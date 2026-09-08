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

    /**
     * 새 값을 저장하고 직전 값을 원자적으로 반환한다 (SET key value PX ttl GET, Redis 6.2+).
     * key 가 없었으면 null. get→set 을 두 번 호출하는 것과 달리 그 사이 다른 요청이
     * 끼어들 틈이 없어, 동시 갱신에서도 각 호출이 서로 다른 직전 값을 정확히 돌려받는다.
     */
    public String setAndGetPrevious(String key, String value, Duration ttl) {
        return redisTemplate.opsForValue().setGet(key, value, ttl);
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
