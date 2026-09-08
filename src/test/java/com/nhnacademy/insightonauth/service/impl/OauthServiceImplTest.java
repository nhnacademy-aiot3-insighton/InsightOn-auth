package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.oauth.LastLoginMethodException;
import com.nhnacademy.insightonauth.exception.oauth.OauthAlreadyLinkedException;
import com.nhnacademy.insightonauth.exception.oauth.OauthNotFoundException;
import com.nhnacademy.insightonauth.exception.user.ReactivationConflictException;
import com.nhnacademy.insightonauth.repository.OauthRepository;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OauthServiceImplTest {

    @Mock
    private OauthRepository oauthRepository;
    @Mock
    private UserCredentialService userCredentialService;

    @InjectMocks
    private OauthServiceImpl oauthService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    private Oauth oauth(String provider, String providerUserId) {
        Oauth oauth = new Oauth(user, provider, providerUserId);
        ReflectionTestUtils.setField(oauth, "oauthId", 10L);
        return oauth;
    }

    @Test
    @DisplayName("create는 Oauth를 저장")
    void create() {
        when(oauthRepository.existsByUserAndProvider(user, "google")).thenReturn(false);

        oauthService.create(user, "google", "pid-1");

        verify(oauthRepository).save(any(Oauth.class));
    }

    @Test
    @DisplayName("create - 이미 같은 provider 연동돼 있으면 예외, save 안 함")
    void create_alreadyLinked() {
        when(oauthRepository.existsByUserAndProvider(user, "google")).thenReturn(true);

        assertThatThrownBy(() -> oauthService.create(user, "google", "pid-1"))
                .isInstanceOf(OauthAlreadyLinkedException.class);

        verify(oauthRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - oauth가 없으면 예외")
    void delete_notFound() {
        when(oauthRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oauthService.delete(user, 10L))
                .isInstanceOf(OauthNotFoundException.class);
    }

    @Test
    @DisplayName("delete - 소유자가 아니면 예외")
    void delete_notOwner() {
        User other = new User("other@test.com", "other", "01099998888");
        ReflectionTestUtils.setField(other, "userId", 2L);
        Oauth oauth = new Oauth(other, "google", "pid-1");
        when(oauthRepository.findById(10L)).thenReturn(Optional.of(oauth));

        assertThatThrownBy(() -> oauthService.delete(user, 10L))
                .isInstanceOf(OauthNotFoundException.class);
    }

    @Test
    @DisplayName("delete - 비밀번호 없고 마지막 연동이면 예외")
    void delete_lastLoginMethod() {
        Oauth oauth = oauth("google", "pid-1");
        when(oauthRepository.findById(10L)).thenReturn(Optional.of(oauth));
        when(userCredentialService.exists(user)).thenReturn(false);
        when(oauthRepository.countByUser(user)).thenReturn(1L);

        assertThatThrownBy(() -> oauthService.delete(user, 10L))
                .isInstanceOf(LastLoginMethodException.class);
    }

    @Test
    @DisplayName("delete - 비밀번호 있으면 마지막 연동이어도 삭제")
    void delete_success_withPassword() {
        Oauth oauth = oauth("google", "pid-1");
        when(oauthRepository.findById(10L)).thenReturn(Optional.of(oauth));
        when(userCredentialService.exists(user)).thenReturn(true);
        when(oauthRepository.countByUser(user)).thenReturn(1L);

        oauthService.delete(user, 10L);

        verify(oauthRepository).delete(oauth);
    }

    @Test
    @DisplayName("deleteAllByUser - repository에 위임")
    void deleteAllByUser() {
        oauthService.deleteAllByUser(user);

        verify(oauthRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("findAllByUser - repository 조회 결과 그대로 반환")
    void findAllByUser() {
        Oauth g = oauth("google", "gid");
        when(oauthRepository.findByUser(user)).thenReturn(List.of(g));

        assertThat(oauthService.findAllByUser(user)).containsExactly(g);
    }

    @Test
    @DisplayName("hasProviderLinked - repository 존재 여부 그대로 반환")
    void hasProviderLinked() {
        when(oauthRepository.existsByUserAndProvider(user, "google")).thenReturn(true);

        assertThat(oauthService.hasProviderLinked(user, "google")).isTrue();
    }

    @Test
    @DisplayName("findByProviderAndProviderUserId - repository 조회 결과 그대로 반환")
    void findByProviderAndProviderUserId() {
        Oauth g = oauth("google", "gid");
        when(oauthRepository.findByProviderAndProviderUserId("google", "gid")).thenReturn(Optional.of(g));

        assertThat(oauthService.findByProviderAndProviderUserId("google", "gid")).contains(g);
    }

    @Test
    @DisplayName("maskByUser - 모든 연동을 마스킹하고 flush")
    void maskByUser() {
        Oauth g = oauth("google", "gid");
        Oauth h = oauth("github", "hid");
        when(oauthRepository.findByUser(user)).thenReturn(List.of(g, h));

        oauthService.maskByUser(user);

        assertThat(g.isMasked()).isTrue();
        assertThat(h.isMasked()).isTrue();
        verify(oauthRepository).flush();
    }

    @Test
    @DisplayName("reactivateByUser - 마스킹 안 된 연동은 건너뜀")
    void reactivateByUser_skipsUnmasked() {
        Oauth g = oauth("google", "gid");   // 마스킹 안 됨
        when(oauthRepository.findByUser(user)).thenReturn(List.of(g));

        oauthService.reactivateByUser(user);

        assertThat(g.getProviderUserId()).isEqualTo("gid");
    }

    @Test
    @DisplayName("reactivateByUser - 원본 식별자가 사용 중이면 예외")
    void reactivateByUser_conflict() {
        Oauth g = oauth("google", "gid");
        g.maskForWithdrawal();
        when(oauthRepository.findByUser(user)).thenReturn(List.of(g));
        when(oauthRepository.findByProviderAndProviderUserId("google", "gid"))
                .thenReturn(Optional.of(oauth("google", "gid")));

        assertThatThrownBy(() -> oauthService.reactivateByUser(user))
                .isInstanceOf(ReactivationConflictException.class);
    }

    @Test
    @DisplayName("reactivateByUser - 충돌 없으면 원본으로 복원")
    void reactivateByUser_unmask() {
        Oauth g = oauth("google", "gid");
        g.maskForWithdrawal();
        when(oauthRepository.findByUser(user)).thenReturn(List.of(g));
        when(oauthRepository.findByProviderAndProviderUserId("google", "gid"))
                .thenReturn(Optional.empty());

        oauthService.reactivateByUser(user);

        assertThat(g.getProviderUserId()).isEqualTo("gid");
        assertThat(g.isMasked()).isFalse();
    }

    @Test
    @DisplayName("findReactivatableByProviderAndProviderUserId - WITHDRAW 상태만 반환")
    void findReactivatable_filtersWithdraw() {
        user.setStatus(Status.WITHDRAW);
        Oauth masked = oauth("google", "gid;abc");
        when(oauthRepository.findByProviderAndProviderUserIdStartingWithOrderByUserWithdrawnAtDesc("google", "gid;"))
                .thenReturn(List.of(masked));

        Optional<Oauth> result =
                oauthService.findReactivatableByProviderAndProviderUserId("google", "gid");

        assertThat(result).containsSame(masked);
    }

    @Test
    @DisplayName("findReactivatableByProviderAndProviderUserId - WITHDRAW 아니면 제외")
    void findReactivatable_excludesNonWithdraw() {
        user.setStatus(Status.ACTIVE);
        Oauth masked = oauth("google", "gid;abc");
        when(oauthRepository.findByProviderAndProviderUserIdStartingWithOrderByUserWithdrawnAtDesc("google", "gid;"))
                .thenReturn(List.of(masked));

        Optional<Oauth> result =
                oauthService.findReactivatableByProviderAndProviderUserId("google", "gid");

        assertThat(result).isEmpty();
    }
}
