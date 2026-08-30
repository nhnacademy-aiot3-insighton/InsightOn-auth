package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.oauth.LastLoginMethodException;
import com.nhnacademy.insightonauth.exception.oauth.OauthNotFoundException;
import com.nhnacademy.insightonauth.exception.user.ReactivationConflictException;
import com.nhnacademy.insightonauth.repository.OauthRepository;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class OauthServiceImpl implements OauthService {

    private final OauthRepository oauthRepository;
    private final UserCredentialService userCredentialService;

    @Override
    public void create(User user, String provider, String providerUserId) {
        Oauth oauth = new Oauth(user, provider, providerUserId);
        oauthRepository.save(oauth);
    }

    // 연동 삭제
    @Override
    public void delete(User user, Long oauthId) {
        Oauth oauth = oauthRepository.findById(oauthId)
                .orElseThrow(() -> new OauthNotFoundException("oauth를 찾을 수 없습니다."));

        if (!oauth.getUser().getUserId().equals(user.getUserId())) {
            throw new OauthNotFoundException("oauth를 찾을 수 없습니다.");
        }

        boolean hasPassword = userCredentialService.exists(user);
        long oauthCount = oauthRepository.countByUser(user);

        // 비밀번호가 없고(OAuth 전용) + 남은 연동이 1개뿐일 때만 막음
        if (!hasPassword && oauthCount <= 1) {
            throw new LastLoginMethodException("마지막 남은 로그인 수단은 해제할 수 없습니다.");
        }

        oauthRepository.delete(oauth);
    }

    @Override
    public void deleteAllByUser(User user) {
        oauthRepository.deleteByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Oauth findOauth(User user, String provider) {
        return oauthRepository.findByUserAndProvider(user, provider)
                .orElseThrow(() -> new OauthNotFoundException("연동된 소셜 계정을 찾을 수 없습니다."));
    }

    // 전체 삭제
    @Override
    @Transactional(readOnly = true)
    public List<Oauth> findAllByUser(User user) {
        return oauthRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Oauth> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return oauthRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProviderLinked(User user, String provider) {
        return oauthRepository.existsByUserAndProvider(user, provider);
    }

    @Override
    public void maskByUser(User user) {
        oauthRepository.findByUser(user).forEach(Oauth::maskForWithdrawal);
        // 같은 트랜잭션에서 곧바로 동일 식별자로 새 연동을 INSERT 하는 경우가 있어,
        // 마스킹 UPDATE 를 먼저 반영해 유니크 충돌을 막는다.
        oauthRepository.flush();
    }

    @Override
    public void reactivateByUser(User user) {
        for (Oauth oauth : oauthRepository.findByUser(user)) {
            if (!oauth.isMasked()) {
                continue;
            }
            String original = oauth.reactivatedProviderUserId();
            if (oauthRepository.findByProviderAndProviderUserId(oauth.getProvider(), original).isPresent()) {
                throw new ReactivationConflictException(
                        "연동된 소셜 계정이 다른 계정에서 사용 중이라 복구할 수 없습니다.");
            }
            oauth.unmask();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Oauth> findReactivatableByProviderAndProviderUserId(String provider, String providerUserId) {
        return oauthRepository
                .findByProviderAndProviderUserIdStartingWithOrderByUserWithdrawnAtDesc(provider, providerUserId + ";")
                .stream()
                .filter(oauth -> oauth.getUser().getStatus() == Status.WITHDRAW)
                .findFirst();
    }
}
