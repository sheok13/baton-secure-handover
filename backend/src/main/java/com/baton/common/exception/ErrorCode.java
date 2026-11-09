package com.baton.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

    // 401
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

    // 403
    DOC_FORBIDDEN_SCOPE(HttpStatus.FORBIDDEN, "문서에 접근할 권한이 없습니다."),

    // 404
    DOC_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화 세션을 찾을 수 없습니다."),

    // 413
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 제한을 초과했습니다."),

    // 423
    AUTH_LOCKED(HttpStatus.LOCKED, "연속된 로그인 실패로 계정이 잠겼습니다. 잠시 후 다시 시도해 주세요."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 502 / 503
    AI_INGEST_FAILED(HttpStatus.BAD_GATEWAY, "AI 문서 처리 중 오류가 발생했습니다."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스에 일시적으로 접근할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

