package com.baton.common;

import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리기 (A 소유).
 *
 * - 프론트엔드 합의 공통 응답 규격({ success: false, error: { ... } }) 준수
 * - 로그인 실패(401, remainingAttempts), 계정 잠금(423, lockedUntil) 정밀 처리
 * - 클라이언트 입력 오류(형식·경로·메서드·Content-Type)는 500이 아닌 4xx로 응답한다 (보안점검 B-8, B-11)
 * - 보안 원칙: 오류 메시지에 내부 클래스명·스택 트레이스·SQL·내부 경로를 노출하지 않고 서버 로그에만 기록
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
     * 타입 변환 실패(예: department=HR)는 Spring 기본 메시지에 내부 클래스명이 들어가므로 일반 문구로 바꾼다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMessage;
        if (fieldError == null) {
            errorMessage = CommonErrorCode.VALIDATION_FAILED.getMessage();
        } else if (fieldError.isBindingFailure()) {
            errorMessage = "'" + fieldError.getField() + "' 값의 형식이 올바르지 않습니다.";
        } else {
            errorMessage = fieldError.getDefaultMessage();
        }

        log.warn("입력값 검증 실패: field={}, bindingFailure={}, message={}",
                (fieldError != null ? fieldError.getField() : "unknown"),
                (fieldError != null && fieldError.isBindingFailure()), errorMessage);

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
     * 경로·쿼리 파라미터 타입 불일치 (예: GET /documents/abc) → 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "'" + e.getName() + "' 값의 형식이 올바르지 않습니다.";
        log.warn("파라미터 타입 불일치: name={}", e.getName());
        return badRequest(message);
    }

    /**
     * 필수 multipart 파트 누락 (예: 업로드에서 file 없음) → 400
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException e) {
        log.warn("필수 파트 누락: part={}", e.getRequestPartName());
        return badRequest("필수 입력값 '" + e.getRequestPartName() + "'이(가) 없습니다.");
    }

    /**
     * 필수 쿼리 파라미터 누락 → 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: param={}", e.getParameterName());
        return badRequest("필수 입력값 '" + e.getParameterName() + "'이(가) 없습니다.");
    }

    /**
     * 지원하지 않는 Content-Type (예: JSON API에 text/plain) → 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    /**
     * 지원하지 않는 HTTP 메서드 (예: DELETE /documents/1) → 405 + Allow 헤더
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("허용되지 않은 메서드: method={}", e.getMethod());
        HttpHeaders headers = new HttpHeaders();
        Set<HttpMethod> allowed = e.getSupportedHttpMethods();
        if (allowed != null && !allowed.isEmpty()) {
            headers.setAllow(allowed);
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 매핑된 API가 없음 (예: GET /api/nope) → 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        log.warn("존재하지 않는 경로 요청: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(CommonErrorCode.NOT_FOUND));
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

    private static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, message));
    }
}