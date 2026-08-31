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
        UserCredential userCredential = userCredentialRepository.findByUser(user)
                .orElseThrow(() -> new UserCredentialsNotFoundException("유저 인증 정보가 없습니다."));

        return userCredential;
    }

    @Override
    public void delete(User user) {
        UserCredential userCredential = findByUser(user);

        userCredentialRepository.delete(userCredential);
    }

    @Override
    public void updatePassword(OffsetDateTime now, User user, String password) {
        UserCredential userCredential = findByUser(user);

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
