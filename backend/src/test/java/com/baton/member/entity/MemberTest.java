package com.baton.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("로그인 실패 시 실패 카운트 증가 및 남은 횟수 반환")
    void recordLoginFailure_incrementsCount() {
        Member member = Member.builder()
                .email("test@baton.local")
                .name("테스트")
                .department(Department.SALES)
                .jobRank(Rank.STAFF)
                .passwordHash("hashed")
                .build();

        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 9, 0, 0);

        int remaining = member.recordLoginFailure(5, 15, now);
        assertThat(remaining).isEqualTo(4);
        assertThat(member.getFailedLoginCount()).isEqualTo(1);
        assertThat(member.isLocked(now)).isFalse();

        // 4회 더 실패 (총 5회)
        for (int i = 0; i < 4; i++) {
            remaining = member.recordLoginFailure(5, 15, now);
        }

        assertThat(remaining).isEqualTo(0);
        assertThat(member.getFailedLoginCount()).isEqualTo(5);
        assertThat(member.getLockedUntil()).isEqualTo(now.plusMinutes(15));
        assertThat(member.isLocked(now)).isTrue();
        assertThat(member.isLocked(now.plusMinutes(16))).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운트 초기화")
    void resetLoginFailure_clearsCountAndLock() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 9, 0, 0);
        Member member = Member.builder()
                .email("test@baton.local")
                .failedLoginCount(5)
                .lockedUntil(now.plusMinutes(15))
                .build();

        member.resetLoginFailure();

        assertThat(member.getFailedLoginCount()).isEqualTo(0);
        assertThat(member.getLockedUntil()).isNull();
        assertThat(member.isLocked(now)).isFalse();
    }
}
