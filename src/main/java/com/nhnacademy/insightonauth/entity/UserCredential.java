package com.nhnacademy.insightonauth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "user_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_credential_id", nullable = false, updatable = false)
    private Long userCredentialId;

    @NonNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, unique = true)
    private User user;

    @NonNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NonNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UserCredential(User user, String passwordHash) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.user = user;
        this.passwordHash = passwordHash;
        this.updatedAt = now;
        this.createdAt = now;
    }

    public void changePassword(OffsetDateTime now, String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = now;
    }
}
