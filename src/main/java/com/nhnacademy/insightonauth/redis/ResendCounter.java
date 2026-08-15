package com.nhnacademy.insightonauth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ResendCounter {

    private final RedisService redisService;

    /**
     * 재전송 횟수를 증가시키고, 임계치를 초과하면 잠금을 건다.
     *
     * @param countKey 카운터 키
     * @param lockKey  잠금 키
     * @param threshold 잠금이 걸리는 임계 횟수
     * @param ttl      카운터/잠금에 적용할 TTL
     */
    public void increase(String countKey, String lockKey, int threshold, Duration ttl) {
        String saved = redisService.get(countKey);
        int count = (saved == null || saved.isBlank()) ? 0 : Integer.parseInt(saved);
        count++;

        if (count >= threshold) {
            redisService.delete(countKey);
            redisService.set(lockKey, "locked", ttl);
        } else {
            redisService.set(countKey, String.valueOf(count), ttl);
        }
    }
}
