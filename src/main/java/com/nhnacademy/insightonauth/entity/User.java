package com.nhnacademy.insightonauth.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @NonNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NonNull
    @Setter
    @Column(name = "email", nullable = false, length = 300, unique = true)
    private String email;

    @NonNull
    @Setter
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Setter
    @Column(name = "phone_number", nullable = true, length = 60, unique = true)
    private String phoneNumber;

    @NonNull
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Setter
    @Column(name = "last_login_at", nullable = true)
    private OffsetDateTime lastLoginAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Setter
    @Column(name = "withdrawn_at", nullable = true)
    private OffsetDateTime withdrawnAt;

    public User(String email, String userName, String phoneNumber) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.userId = UuidCreator.getTimeOrderedEpoch();
        this.email = email;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.status = Status.ACTIVE;
        this.updatedAt = now;
        this.createdAt = now;
    }

    public void withdraw() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.email = email + ";" + this.userId;
        if (this.phoneNumber != null) {
            this.phoneNumber = this.phoneNumber + ";" + this.userId;
        }
        this.status = Status.WITHDRAW;
        this.updatedAt = now;
        this.withdrawnAt = now;
    }
}
