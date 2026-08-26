package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserNameAndPhoneNumber(String userName, String phoneNumber);

    Optional<User> findByEmailStartingWithAndStatus(String email, Status status);

    Page<User> findByEmailContainingAndUserNameContainingAndStatus(
            String email, String userName, Status status, Pageable pageable);

    Page<User> findByEmailContainingAndUserNameContaining(
            String email, String userName, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByStatusAndWithdrawnAtBefore(Status status, OffsetDateTime withdrawnAtBefore);

    List<User> findByStatusAndLastLoginAtBefore(Status status, OffsetDateTime dateTime);
}
