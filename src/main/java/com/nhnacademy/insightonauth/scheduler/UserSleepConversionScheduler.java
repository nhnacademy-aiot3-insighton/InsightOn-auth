package com.nhnacademy.insightonauth.scheduler;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSleepConversionScheduler {

    private final RedissonClient redissonClient;
    private final UserManagementService userManagementService;

    @Scheduled(cron = "0 0 2 * * *")   // 매일 새벽 2시
    // Redisson RLock.unlock()은 확인~해제 사이 워치독 갱신 실패로 소유권을 잃으면
    // IllegalMonitorStateException을 던지는 게 공식 동작이라, 그 경우만 로그로 흡수한다.
    @SuppressWarnings("java:S2235")
    public void convertInactiveUsersToSleep() {
        // 이중화된 여러 인스턴스가 동시에 이 배치를 실행하지 않도록 Redisson 분산 락 사용.
        // 워치독이 작업 시간에 맞춰 락 TTL을 자동 갱신하므로, 배치가 오래 걸려도
        // 락이 만료돼 다른 인스턴스가 끼어드는 일이 없고, 소유권 확인 해제로 남의 락 삭제도 방지됨.
        // 락 객체(핸들) 획득 — 실제 잠금은 tryLock에서 수행
        RLock lock = redissonClient.getLock(RedisKey.SLEEP_CONVERSION_SCHEDULER_LOCK.getPrefix());

        boolean acquired;
        try {
            // waitTime=0  : 이미 다른 인스턴스가 잡았으면 대기 없이 즉시 실패(false) → 스킵
            // leaseTime=-1: 워치독 활성화. 고정 만료 대신, 인스턴스가 살아있는 동안 TTL을 자동 갱신
            acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("휴면 전환 락 획득 중 인터럽트 발생");
            return;
        }

        if (!acquired) {
            log.info("다른 인스턴스가 이미 휴면 전환 작업을 실행 중입니다. 건너뜁니다.");
            return;
        }

        try {
            List<User> targets = userManagementService.findInactiveUsers();

            for (User user : targets) {
                try {
                    userManagementService.sleep(user.getUserId());
                } catch (Exception e) {
                    // 하나가 실패해도 나머지는 진행되게
                    log.warn("휴면 전환 실패 - userId: {}, error: {}", user.getUserId(), e.getMessage());
                }
            }

            log.info("휴면 전환 완료 - 대상 {}건", targets.size());
        } finally {
            // 내가 쥔 락일 때만 해제 (혹시 만료로 소유권이 넘어갔으면 건드리지 않음 → 예외/남의 락 삭제 방지)
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // 확인~해제 사이 워치독 갱신 실패로 락이 만료돼 소유권을 잃은 경우
                    log.warn("락 해제 실패 - 이미 소유권을 상실함 (TTL 만료 추정). lockKey={}",
                            RedisKey.SLEEP_CONVERSION_SCHEDULER_LOCK.getPrefix());
                }
            }
        }
    }
}
