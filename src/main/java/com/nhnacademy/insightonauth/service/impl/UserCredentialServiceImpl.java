package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.exception.UserCredentialsNotFoundException;
import com.nhnacademy.insightonauth.repository.UserCredentialRepository;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public UserCredential findByUser(User user) {
        UserCredential userCredential = userCredentialRepository.findByUser(user)
                .orElseThrow(() -> new UserCredentialsNotFoundException("유저 인증 정보가 없습니다."));

        return userCredential;
    }
}
