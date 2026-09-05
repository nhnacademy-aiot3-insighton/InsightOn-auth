package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.exception.user.SameAsOldPasswordException;
import com.nhnacademy.insightonauth.exception.user.UserCredentialsNotFoundException;
import com.nhnacademy.insightonauth.repository.UserCredentialRepository;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCredentialServiceImpl implements UserCredentialService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void create(User user, String password) {
        String hash = passwordEncoder.encode(password);
        userCredentialRepository.save(new UserCredential(user, hash));
    }

    @Override
    @Transactional(readOnly = true)
    public UserCredential findByUser(User user) {
        return userCredentialRepository.findByUser(user)
                .orElseThrow(() -> new UserCredentialsNotFoundException("유저 인증 정보가 없습니다."));
    }

    @Override
    public void delete(User user) {
        // OAuth 전용 가입 계정은 애초에 크리덴셜이 없을 수 있다(비밀번호 미설정) — 계정 삭제 시
        // "지울 게 있으면 지운다"가 맞고, findByUser()처럼 없다고 예외를 던지면 안 된다.
        userCredentialRepository.findByUser(user).ifPresent(userCredentialRepository::delete);
    }

    @Override
    public void updatePassword(OffsetDateTime now, User user, String password) {
        // self-invocation으로 findByUser()의 @Transactional(readOnly=true)가 무시되는 걸 피하려고 직접 조회
        UserCredential userCredential = userCredentialRepository.findByUser(user)
                .orElseThrow(() -> new UserCredentialsNotFoundException("유저 인증 정보가 없습니다."));

        // 마이페이지 변경 / 이메일 재설정 양쪽 모두 여기서 기존 비밀번호와 동일 여부를 검사한다
        if (passwordEncoder.matches(password, userCredential.getPasswordHash())) {
            throw new SameAsOldPasswordException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        userCredential.changePassword(now, passwordEncoder.encode(password));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(User user) {
        return userCredentialRepository.existsByUser(user);
    }
}
