package com.baton.common;

import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 인터페이스 (A 소유).
 * 각 도메인 에러코드 Enum(AuthErrorCode, DocumentErrorCode, ChatErrorCode, AiErrorCode 등)은
 * 이 인터페이스를 구현한다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}

