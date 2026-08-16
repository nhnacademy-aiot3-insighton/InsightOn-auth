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
public class UserHardDeleteScheduler {

    private final RedissonClient redissonClient;
    private final UserManagementService userManagementService;

    @Scheduled(cron = "0 0 1 * * *")   // 매일 새벽 1시
    public void hardDeleteExpiredUsers() {
        RLock lock = redissonClient.getLock(RedisKey.HARD_DELETE_SCHEDULER_LOCK.getPrefix());

        boolean acquired;
        try {
            // waitTime=0 : 이미 남이 잡았으면 기다리지 않고 즉시 스킵
            // leaseTime=-1 : 워치독 활성화 (작업이 끝날 때까지 TTL 자동 갱신)
            acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("하드 삭제 락 획득 중 인터럽트 발생");
            return;
        }

        if (!acquired) {
            log.info("다른 인스턴스가 이미 이 작업을 실행 중입니다. 건너뜁니다.");
            return;
        }

        try {
            List<User> targets = userManagementService.findExpiredWithdrawnUsers();

            for (User user : targets) {
                try {
                    userManagementService.deleteUser(user.getUserId());
                } catch (Exception e) {
                    log.warn("탈퇴 계정 삭제 실패 - userId: {}, error: {}", user.getUserId(), e.getMessage());
                }
            }

            log.info("탈퇴 계정 물리 삭제 완료 - 대상 {}건", targets.size());
        } finally {
            // 내가 쥔 락일 때만 해제 (남의 락/이미 만료된 락은 건드리지 않음)
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // 확인~해제 사이 워치독 갱신 실패로 락이 만료돼 소유권을 잃은 경우
                    log.warn("락 해제 실패 - 이미 소유권을 상실함 (TTL 만료 추정). lockKey={}",
                            RedisKey.HARD_DELETE_SCHEDULER_LOCK.getPrefix());
                }
            }
        }
    }
}
