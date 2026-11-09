package com.baton.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private final String secret = "this-is-a-very-secret-key-that-is-at-least-32-bytes-long!";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(secret, 1800000L, 604800000L);
        jwtProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성 및 uuid 추출 검증")
    void createAccessToken_and_getUuid() {
        String testUuid = UUID.randomUUID().toString();

        String token = jwtProvider.createAccessToken(testUuid);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUuid(token)).isEqualTo(testUuid);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 false 반환")
    void validateToken_expired() {
        JwtProvider expiredProvider = new JwtProvider(secret, -1000L, 604800000L);
        expiredProvider.init();

        String token = expiredProvider.createAccessToken("test-uuid");
        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("변조된 토큰 검증 시 false 반환")
    void validateToken_tampered() {
        String token = jwtProvider.createAccessToken("test-uuid");
        String tamperedToken = token + "tampered";

        assertThat(jwtProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 원문 생성 시 43자 이상 난수 문자열 생성")
    void createRefreshToken() {
        String refreshToken1 = jwtProvider.createRefreshToken();
        String refreshToken2 = jwtProvider.createRefreshToken();

        assertThat(refreshToken1).isNotBlank();
        assertThat(refreshToken2).isNotBlank();
        assertThat(refreshToken1).isNotEqualTo(refreshToken2);
    }
}

