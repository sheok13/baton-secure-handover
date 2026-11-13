package com.baton.common.dto;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

    private String code;
    private String message;
    private Integer remainingAttempts;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lockedUntil;

    public static ErrorDetail of(ErrorCode errorCode) {
        return ErrorDetail.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorDetail of(ErrorCode errorCode, String customMessage) {
        return ErrorDetail.builder()
                .code(errorCode.getCode())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .build();
    }

    public static ErrorDetail of(String code, String message) {
        return ErrorDetail.builder()
                .code(code)
                .message(message)
                .build();
    }

    public static ErrorDetail loginFailure(int remainingAttempts) {
        return ErrorDetail.builder()
                .code(AuthErrorCode.AUTH_INVALID_CREDENTIALS.getCode())
                .message(AuthErrorCode.AUTH_INVALID_CREDENTIALS.getMessage())
                .remainingAttempts(remainingAttempts)
                .build();
    }

    public static ErrorDetail accountLocked(OffsetDateTime lockedUntil) {
        return ErrorDetail.builder()
                .code(AuthErrorCode.AUTH_LOCKED.getCode())
                .message(AuthErrorCode.AUTH_LOCKED.getMessage())
                .lockedUntil(lockedUntil)
                .build();
    }
}
