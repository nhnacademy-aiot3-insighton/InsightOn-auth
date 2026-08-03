package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OauthRepository extends JpaRepository<Oauth, Long> {

    Optional<Oauth> findByUserAndProvider(User user, String provider);

    List<Oauth> findByUser(User user);

    Long countByUser(User user);
}
