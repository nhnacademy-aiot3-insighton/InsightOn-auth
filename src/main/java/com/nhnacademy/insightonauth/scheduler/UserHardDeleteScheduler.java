package com.nhnacademy.insightonauth.scheduler;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHardDeleteScheduler {

    private final UserService userService;
    private final RedisService redisService;

    @Scheduled(cron = "0 0 1 * * *")
    public void hardDeleteExpiredUsers() {
        String lockKey = RedisKey.HARD_DELETE_SCHEDULER_LOCK.getPrefix();
        boolean acquired = redisService.setIfAbsent(lockKey, "locked", Duration.ofMinutes(30));

        if (!acquired) {
            log.info("다른 인스턴스가 이미 이 작업을 실행 중입니다. 건너뜁니다.");
            return;
        }

        try {
            List<User> targets = userService.findExpiredWithdrawnUsers();

            for (User user : targets) {
                try {
                    userService.deleteUser(user.getUserId());
                } catch (Exception e) {
                    log.warn("탈퇴 계정 삭제 실패 - userId: {}, error: {}", user.getUserId(), e.getMessage());
                }
            }

            log.info("탈퇴 계정 물리 삭제 완료 - 대상 {}건", targets.size());
        } finally {
            redisService.delete(lockKey);
        }
    }
}
