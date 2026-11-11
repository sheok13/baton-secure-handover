package com.baton.common;

import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기 (A 소유).
 * 
 * - 프론트엔드 합의 공통 응답 규격({ success: false, error: { ... } }) 준수
 * - 로그인 실패(401, remainingAttempts), 계정 잠금(423, lockedUntil) 정밀 처리
 * - 보안 원칙: 500 내부 오류 시 스택 트레이스·SQL·내부 경로를 노출하지 않고 서버 로그에만 기록
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 로그인 실패 (401 Unauthorized, remainingAttempts 포함)
     */
    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginFailed(LoginFailedException e) {
        log.warn("로그인 실패 예외: remainingAttempts={}, message={}", e.getRemainingAttempts(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.loginFailure(e.getRemainingAttempts()));
    }

    /**
     * 계정 잠금 (423 Locked, lockedUntil 포함)
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException e) {
        log.warn("계정 잠금 예외: lockedUntil={}, message={}", e.getLockedUntil(), e.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.accountLocked(e.getLockedUntil()));
    }

    /**
     * 도메인 비즈니스 예외 (문서함 404, 채팅 404, AI 오류 등)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생: code={}, status={}, message={}",
                errorCode.getCode(), errorCode.getStatus(), e.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /**
     * DTO 유효성 검증 실패 (400 Bad Request, @Valid 오류)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : CommonErrorCode.VALIDATION_FAILED.getMessage();

        log.warn("입력값 검증 실패: field={}, message={}",
                (fieldError != null ? fieldError.getField() : "unknown"), errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, errorMessage));
    }

    /**
     * 잘못된 HTTP 요청 본문 형식 (400 Bad Request, 깨진 JSON 파싱 실패 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패 (잘못된 JSON 형식): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "요청 본문 형식이 올바르지 않습니다."));
    }

    /**
     * 기타 예기치 못한 서버 내부 오류 (500 Internal Server Error)
     * 보안: 상세 스택 트레이스는 서버 로그에만 기록하고 클라이언트에는 일반 메시지만 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[SYSTEM_ERROR] 처리되지 않은 예외 발생: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR));
    }
}

