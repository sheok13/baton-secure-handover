package com.baton.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 생성, 서명 및 검증 컴포넌트 (A 소유).
 * 
 * - Access Token: 30분, sub = member.uuid (내부 DB id 노출 방지)
 * - Refresh Token: 7일, 암호학적 난수 생성 후 DB에는 SHA-256 해시만 저장
 */
@Slf4j
@Component
public class JwtProvider {

    private final String secretKeyString;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey key;

    public JwtProvider(
            @Value("${jwt.secret}") String secretKeyString,
            @Value("${jwt.access-expiration:1800000}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs
    ) {
        this.secretKeyString = secretKeyString;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostConstruct
    public void init() {
        if (secretKeyString == null || secretKeyString.isBlank()) {
            throw new IllegalArgumentException("jwt.secret (JWT_SECRET_KEY) 환경변수가 설정되지 않았습니다.");
        }
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret (JWT_SECRET_KEY)은 최소 32바이트(256비트) 이상이어야 합니다. 현재: " + keyBytes.length + "바이트");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 발급 (sub = member.uuid)
     */
    public String createAccessToken(String uuid) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(uuid)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token 원문 생성 (암호학적으로 안전한 32바이트 Base64 문자열)
     */
    public String createRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 토큰에서 외부 식별자(uuid) 추출
     */
    public String getUuid(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 유효성 및 만료 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token 만료 시간(초 단위) 반환
     */
    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /**
     * Refresh Token 만료 시간(밀리초) 반환
     */
    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

