package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    boolean existsByUser(User user);

    Optional<UserCredential> findByUser(User user);
}
