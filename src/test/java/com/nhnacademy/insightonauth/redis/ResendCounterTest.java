package com.nhnacademy.insightonauth.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendCounterTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ResendCounter resendCounter;

    @Test
    @DisplayName("임계치 미만이면 잠금 없이 false 반환")
    void increase_belowThreshold() {
        when(redisService.increment("count", Duration.ofMinutes(15))).thenReturn(3L);

        boolean locked = resendCounter.increase("count", "lock", 5, Duration.ofMinutes(15));

        assertThat(locked).isFalse();
        verify(redisService, never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("임계치에 도달하면 카운터를 지우고 잠금을 걸고 true 반환")
    void increase_reachesThreshold() {
        when(redisService.increment("count", Duration.ofMinutes(15))).thenReturn(5L);

        boolean locked = resendCounter.increase("count", "lock", 5, Duration.ofMinutes(15));

        assertThat(locked).isTrue();
        verify(redisService).delete("count");
        verify(redisService).set("lock", "locked", Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("임계치를 초과해도(이미 넘은 상태) true 반환")
    void increase_aboveThreshold() {
        when(redisService.increment("count", Duration.ofMinutes(15))).thenReturn(6L);

        boolean locked = resendCounter.increase("count", "lock", 5, Duration.ofMinutes(15));

        assertThat(locked).isTrue();
    }
}
