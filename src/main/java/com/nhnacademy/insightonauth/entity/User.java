package com.nhnacademy.insightonauth.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nhnacademy.insightonauth.exception.InvalidUserStatusException;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

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

        this.email = email;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.status = Status.ACTIVE;
        this.updatedAt = now;
        this.createdAt = now;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateLastLoginAt(OffsetDateTime lastLoginAt) {  // 테스트/특정 시각 지정용
        this.lastLoginAt = lastLoginAt;
    }

    public void reactivate() {
        if (this.status != Status.SLEEP && this.status != Status.WITHDRAW) {
            throw new InvalidUserStatusException("휴면 또는 탈퇴 상태가 아닙니다.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 탈퇴 상태였다면, 이메일/전화번호 복원
        if (this.status == Status.WITHDRAW) {
            this.email = reactivatedEmail();
            if (this.phoneNumber != null) {
                this.phoneNumber = reactivatedPhoneNumber();
            }
            this.withdrawnAt = null;
        }

        this.status = Status.ACTIVE;
        this.updatedAt = now;
        this.lastLoginAt = now;
    }

    /** 마스킹 접미사를 뗀 원본 이메일 (마스킹 안 됐으면 그대로). */
    public String reactivatedEmail() {
        return this.email.split(";")[0];
    }

    /** 마스킹 접미사를 뗀 원본 전화번호 (null이면 null). */
    public String reactivatedPhoneNumber() {
        return this.phoneNumber == null ? null : this.phoneNumber.split(";")[0];
    }

    public void withdraw() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID uuid = UuidCreator.getTimeOrderedEpoch();

        this.email = email + ";" + uuid;
        if (this.phoneNumber != null) {
            this.phoneNumber = this.phoneNumber + ";" + uuid;
        }
        this.status = Status.WITHDRAW;
        this.updatedAt = now;
        this.withdrawnAt = now;
    }
}
