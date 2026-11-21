package com.baton.auth.entity;

import com.baton.common.BaseTimeEntity;
import com.baton.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static RefreshToken create(Member member, String tokenHash, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .member(member)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 토큰 폐기 (로그아웃, Rotation 재발급 시)
     */
    public void revoke(LocalDateTime now) {
        this.revokedAt = now;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }

    /**
     * 이미 폐기된 토큰인지 확인
     */
    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    /**
     * 토큰이 유효한지 확인 (폐기되지 않고 만료되지 않음)
     */
    public boolean isValid(LocalDateTime now) {
        return !isRevoked() && !isExpired(now);
    }
}

