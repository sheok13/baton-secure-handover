package com.baton.member.entity;

import com.baton.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "department", nullable = false, length = 10)
    private Department department;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "job_rank", nullable = false, length = 10)
    private Rank jobRank;

    @Builder.Default
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @PrePersist
    public void initMember() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    /**
     * 계정 잠금 여부 확인
     */
    public boolean isLocked(LocalDateTime now) {
        return this.lockedUntil != null && now.isBefore(this.lockedUntil);
    }

    /**
     * 로그인 실패 기록 및 연속 실패 횟수에 따른 잠금 처리
     * @return 남은 시도 가능 횟수 (0이면 잠금 처리됨)
     */
    public int recordLoginFailure(int maxAttempts, int lockDurationMinutes, LocalDateTime now) {
        if (this.lockedUntil != null && !now.isBefore(this.lockedUntil)) {
            this.failedLoginCount = 0;
            this.lockedUntil = null;
        }

        this.failedLoginCount++;
        int remaining = Math.max(0, maxAttempts - this.failedLoginCount);

        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = now.plusMinutes(lockDurationMinutes);
        }

        return remaining;
    }

    /**
     * 로그인 성공 시 실패 카운트 및 잠금 해제 초기화
     */
    public void resetLoginFailure() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }
}
