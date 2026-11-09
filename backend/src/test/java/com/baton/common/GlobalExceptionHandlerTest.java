package com.baton.common;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("LoginFailedException 발생 시 401과 remainingAttempts를 담은 응답 반환")
    void handleLoginFailed() {
        LoginFailedException ex = new LoginFailedException(3);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleLoginFailed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(response.getBody().getError().getRemainingAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("AccountLockedException 발생 시 423과 lockedUntil을 담은 응답 반환")
    void handleAccountLocked() {
        OffsetDateTime lockedUntil = OffsetDateTime.of(2026, 11, 9, 14, 0, 0, 0, ZoneOffset.ofHours(9));
        AccountLockedException ex = new AccountLockedException(lockedUntil);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccountLocked(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("AUTH_LOCKED");
        assertThat(response.getBody().getError().getLockedUntil()).isEqualTo(lockedUntil);
    }

    @Test
    @DisplayName("BusinessException 발생 시 해당 에러코드의 HTTP 상태코드와 메시지 반환")
    void handleBusinessException() {
        BusinessException ex = new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID, "토큰이 만료되었습니다.");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("AUTH_TOKEN_INVALID");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("토큰이 만료되었습니다.");
    }

    @Test
    @DisplayName("기타 예외 발생 시 500 INTERNAL_ERROR 반환 (스택트레이스 숨김)")
    void handleException() {
        Exception ex = new RuntimeException("DB 연결 끊김 등 알 수 없는 에러");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }
}

