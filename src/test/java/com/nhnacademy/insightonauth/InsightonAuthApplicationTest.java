package com.nhnacademy.insightonauth;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class InsightonAuthApplicationTest {

    // 스케줄러(UserHardDeleteScheduler 등)가 주입받는 RedissonClient는 빈 생성 시점에
    // 즉시 실제 Redis 연결을 시도하기 때문에, 컨텍스트 로드만 확인하는 이 테스트에서는
    // 실 연결 없이 목으로 대체한다.
    @MockitoBean
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        // 본문은 의도적으로 비어 있음 — 스프링 컨텍스트 로드 자체가 실패하면
        // @Test 메서드에 도달하기도 전에 실패하므로, 여기까지 오면 이 테스트는 통과다.
    }
}
