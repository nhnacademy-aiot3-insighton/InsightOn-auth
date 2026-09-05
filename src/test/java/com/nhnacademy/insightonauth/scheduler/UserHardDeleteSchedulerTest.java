package com.nhnacademy.insightonauth.scheduler;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.service.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserHardDeleteSchedulerTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private RLock lock;

    @InjectMocks
    private UserHardDeleteScheduler scheduler;

    @Test
    @DisplayName("락 획득 성공 시 대상 전부 삭제하고 락 해제")
    void hardDelete_success() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        User user1 = new User("a@test.com", "a", "01011111111");
        ReflectionTestUtils.setField(user1, "userId", 1L);
        User user2 = new User("b@test.com", "b", "01022222222");
        ReflectionTestUtils.setField(user2, "userId", 2L);
        when(userManagementService.findExpiredWithdrawnUsers()).thenReturn(List.of(user1, user2));

        scheduler.hardDeleteExpiredUsers();

        verify(userManagementService).deleteUser(1L);
        verify(userManagementService).deleteUser(2L);
        verify(lock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패하면 삭제 로직을 아예 실행하지 않음")
    void hardDelete_lockNotAcquired() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.hardDeleteExpiredUsers();

        verifyNoInteractions(userManagementService);
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("한 명 삭제가 실패해도 나머지는 계속 처리됨")
    void hardDelete_partialFailure() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        User user1 = new User("a@test.com", "a", "01011111111");
        ReflectionTestUtils.setField(user1, "userId", 1L);
        User user2 = new User("b@test.com", "b", "01022222222");
        ReflectionTestUtils.setField(user2, "userId", 2L);
        when(userManagementService.findExpiredWithdrawnUsers()).thenReturn(List.of(user1, user2));
        doThrow(new RuntimeException("boom")).when(userManagementService).deleteUser(1L);

        scheduler.hardDeleteExpiredUsers();

        verify(userManagementService).deleteUser(1L);
        verify(userManagementService).deleteUser(2L);
        verify(lock).unlock();
    }

    @Test
    @DisplayName("인터럽트로 락 획득 실패하면 인터럽트 플래그만 세우고 종료")
    void hardDelete_interrupted() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        try {
            scheduler.hardDeleteExpiredUsers();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verifyNoInteractions(userManagementService);
            verify(lock, never()).unlock();
        } finally {
            Thread.interrupted(); // 인터럽트 플래그 초기화 (다른 테스트 오염 방지)
        }
    }

    @Test
    @DisplayName("소유권을 잃은 락이면 unlock을 호출하지 않음")
    void hardDelete_lockLostOwnership() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);
        when(userManagementService.findExpiredWithdrawnUsers()).thenReturn(List.of());

        scheduler.hardDeleteExpiredUsers();

        verify(lock, never()).unlock();
    }
}
