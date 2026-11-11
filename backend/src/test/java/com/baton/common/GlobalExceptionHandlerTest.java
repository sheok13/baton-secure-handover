package com.baton.common;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // ===== 보안점검 B-11: 클라이언트 입력 오류는 4xx, 내부 정보 미노출 =====

    @Test
    @DisplayName("B-11-03: enum 변환 실패(department=HR) → 400, 메시지에 내부 클래스명 미노출")
    void bindingFailureHidesInternalClassName() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "department", "HR", true,
                new String[]{"typeMismatch"}, null,
                "Failed to convert value of type 'java.lang.String' to required type 'com.baton.member.entity.Department'"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), result);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getMessage())
                .isEqualTo("'department' 값의 형식이 올바르지 않습니다.")
                .doesNotContain("com.baton", "java.lang");
    }

    @Test
    @DisplayName("B-11-04: 일반 검증 실패(@Size 등)는 기존처럼 어노테이션 메시지 유지")
    void constraintViolationKeepsDefaultMessage() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "title", "크기가 0에서 200 사이여야 합니다"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), result);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("크기가 0에서 200 사이여야 합니다");
    }

    @Test
    @DisplayName("B-11-05: 경로 변수 타입 불일치(/documents/abc) → 400")
    void typeMismatchReturns400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("'id' 값의 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("B-11-07: 깨진 JSON 본문 → 400, 파서 오류 내용 미노출")
    void notReadableReturns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected end-of-input at [Source: ...]", mock(HttpInputMessage.class));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getMessage())
                .isEqualTo("요청 본문의 형식이 올바르지 않습니다.")
                .doesNotContain("JSON parse error");
    }

    @Test
    @DisplayName("B-11-08: 지원하지 않는 Content-Type(text/plain) → 415")
    void mediaTypeNotSupportedReturns415() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("text/plain not supported");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMediaTypeNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().getError().getCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    @DisplayName("B-11-09: 매핑되지 않은 경로(/api/nope) → 404")
    void noResourceReturns404() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getResourcePath()).thenReturn("api/nope");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("B-11-10: 지원하지 않는 메서드(DELETE) → 405 + Allow 헤더")
    void methodNotSupportedReturns405WithAllowHeader() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET"));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.GET);
        assertThat(response.getBody().getError().getCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }
}