package com.nhnacademy.insightonauth.redis;

import com.nhnacademy.insightonauth.exception.external.RedisOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        redisService = new RedisService(redisTemplate);
    }

    @Test
    @DisplayName("set - ValueOperations로 TTL과 함께 저장")
    void set() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.set("key", "value", Duration.ofMinutes(5));

        verify(valueOperations).set("key", "value", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("get - 값 조회")
    void get() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");

        assertThat(redisService.get("key")).isEqualTo("value");
    }

    @Test
    @DisplayName("delete - 키 삭제")
    void delete() {
        redisService.delete("key");

        verify(redisTemplate).delete("key");
    }

    @Test
    @DisplayName("hasKey - true면 true 반환")
    void hasKey_true() {
        when(redisTemplate.hasKey("key")).thenReturn(true);

        assertThat(redisService.hasKey("key")).isTrue();
    }

    @Test
    @DisplayName("hasKey - null이면 false 반환 (NPE 없이)")
    void hasKey_null() {
        when(redisTemplate.hasKey("key")).thenReturn(null);

        assertThat(redisService.hasKey("key")).isFalse();
    }

    @Test
    @DisplayName("getAndDelete - GETDEL 위임")
    void getAndDelete() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("key")).thenReturn("value");

        assertThat(redisService.getAndDelete("key")).isEqualTo("value");
    }

    @Test
    @DisplayName("setAndGetPrevious - 직전 값 원자적으로 반환")
    void setAndGetPrevious() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setGet("key", "new", Duration.ofMinutes(1))).thenReturn("old");

        assertThat(redisService.setAndGetPrevious("key", "new", Duration.ofMinutes(1))).isEqualTo("old");
    }

    @Test
    @DisplayName("setIfAbsent - 성공하면 true")
    void setIfAbsent_true() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(60))).thenReturn(true);

        assertThat(redisService.setIfAbsent("key", "value", Duration.ofSeconds(60))).isTrue();
    }

    @Test
    @DisplayName("setIfAbsent - 이미 있으면 false")
    void setIfAbsent_false() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(60))).thenReturn(false);

        assertThat(redisService.setIfAbsent("key", "value", Duration.ofSeconds(60))).isFalse();
    }

    @Test
    @DisplayName("setIfAbsent - null 응답이면 false (NPE 없이)")
    void setIfAbsent_null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(60))).thenReturn(null);

        assertThat(redisService.setIfAbsent("key", "value", Duration.ofSeconds(60))).isFalse();
    }

    @Test
    @DisplayName("increment - 첫 증가(1)면 TTL을 설정")
    void increment_first() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("key")).thenReturn(1L);

        long result = redisService.increment("key", Duration.ofMinutes(5));

        assertThat(result).isEqualTo(1L);
        verify(redisTemplate).expire("key", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("increment - 2회차부터는 TTL을 다시 설정하지 않음")
    void increment_subsequent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("key")).thenReturn(2L);

        long result = redisService.increment("key", Duration.ofMinutes(5));

        assertThat(result).isEqualTo(2L);
        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    @DisplayName("increment - null 응답이면 예외")
    void increment_null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("key")).thenReturn(null);
        Duration ttl = Duration.ofMinutes(5);

        assertThatThrownBy(() -> redisService.increment("key", ttl))
                .isInstanceOf(RedisOperationException.class);
    }
}
