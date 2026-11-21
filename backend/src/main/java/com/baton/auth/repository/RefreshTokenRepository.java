package com.baton.auth.repository;

import com.baton.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 회원의 유효한 모든 리프레시 토큰 일괄 폐기 (비정상 접근 감지 시)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.member.id = :memberId AND r.revokedAt IS NULL")
    void revokeAllByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}

