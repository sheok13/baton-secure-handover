package com.baton.chat.exception;

import com.baton.common.ErrorCode;
import org.springframework.http.HttpStatus;

/** 채팅 오류 코드 (API 설계서 5·7장) */
public enum ChatErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "대화를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ChatErrorCode(HttpStatus status, String code, String message) {
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
