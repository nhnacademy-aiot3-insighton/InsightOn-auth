package com.nhnacademy.insightonauth.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nhnacademy.insightonauth.util.WithdrawalMask;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "oauths", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_oauths_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(
                name = "uq_oauth_user_provider",
                columnNames = {"user_id", "provider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Oauth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id", nullable = false, updatable = false)
    private Long oauthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Oauth(User user, String provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void reassignUser(User newUser) {
        this.user = newUser;
    }

    /**
     * 탈퇴 시 provider_user_id 에 접미사를 붙여 유니크 제약을 비켜준다.
     * 이미 마스킹돼 있으면 아무것도 하지 않는다.
     */
    public void maskForWithdrawal() {
        if (isMasked()) {
            return;
        }
        this.providerUserId = this.providerUserId + ";" + UuidCreator.getTimeOrderedEpoch();
    }

    /** 복구 시 원본 provider_user_id 로 되돌린다. */
    public void unmask() {
        this.providerUserId = reactivatedProviderUserId();
    }

    public boolean isMasked() {
        return WithdrawalMask.isMasked(this.providerUserId);
    }

    /** 마스킹 접미사를 뗀 원본 provider_user_id (마스킹 안 됐으면 그대로). */
    public String reactivatedProviderUserId() {
        return WithdrawalMask.strip(this.providerUserId);
    }
}
