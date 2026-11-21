package com.baton.ai.exception;

import com.baton.common.ErrorCode;
import org.springframework.http.HttpStatus;

/** AI 모듈 연동 오류 코드 (API 설계서 7장). AI 쪽 원본 오류는 FE에 그대로 노출하지 않는다 */
public enum AiErrorCode implements ErrorCode {

    AI_INGEST_FAILED(HttpStatus.BAD_GATEWAY, "AI_INGEST_FAILED", "문서 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
