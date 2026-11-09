package com.baton.auth.service;

import com.baton.auth.entity.RefreshToken;
import com.baton.auth.exception.AuthErrorCode;
import com.baton.auth.repository.RefreshTokenRepository;
import com.baton.common.BusinessException;
import com.baton.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * Refresh Token 관리 서비스 (A 소유).
 * 
 * - 보안: DB에 토큰 원문을 저장하지 않고 SHA-256 해시값만 저장
 * - Rotation: 재발급 시 기존 토큰 폐기 및 새 토큰 발급
 * - 재사용 감지: 이미 폐기된 토큰 사용 시 탈취로 간주하고 해당 회원의 모든 유효 토큰 일괄 폐기
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 리프레시 토큰 SHA-256 해싱 및 저장
     */
    @Transactional
    public void saveRefreshToken(Member member, String rawToken, LocalDateTime expiresAt) {
        String tokenHash = hashToken(rawToken);
        RefreshToken refreshToken = RefreshToken.create(member, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 리프레시 토큰 검증 및 교체 (Rotation)
     * @return 토큰 소유 회원
     */
    @Transactional
    public Member validateAndRotate(String rawToken, String newRawToken, LocalDateTime newExpiresAt) {
        String tokenHash = hashToken(rawToken);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID));

        // 재사용 감지: 이미 폐기된 토큰이 다시 사용된 경우 (토큰 탈취 시나리오)
        if (token.isRevoked()) {
            log.warn("[AUDIT] REFRESH_TOKEN_REUSE_DETECTED memberId={} tokenHash={}",
                    token.getMember().getId(), tokenHash);
            // 해당 회원의 모든 활성 리프레시 토큰 일괄 폐기
            refreshTokenRepository.revokeAllByMemberId(token.getMember().getId(), now);
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID);
        }

        // 만료 검사
        if (token.isExpired(now)) {
            log.info("만료된 Refresh Token 요청입니다: tokenHash={}", tokenHash);
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID);
        }

        // 기존 토큰 폐기 및 새 토큰 발급 (Rotation)
        token.revoke(now);
        Member member = token.getMember();
        saveRefreshToken(member, newRawToken, newExpiresAt);

        return member;
    }

    /**
     * 리프레시 토큰 폐기 (로그아웃)
     */
    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    /**
     * SHA-256 해시 함수
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 지원하지 않는 환경입니다.", e);
        }
    }
}

