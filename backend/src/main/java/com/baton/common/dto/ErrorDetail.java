package com.baton.common.dto;

import com.baton.common.exception.ErrorCode;
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
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorDetail of(ErrorCode errorCode, String customMessage) {
        return ErrorDetail.builder()
                .code(errorCode.name())
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
                .code(ErrorCode.AUTH_INVALID_CREDENTIALS.name())
                .message(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage())
                .remainingAttempts(remainingAttempts)
                .build();
    }

    public static ErrorDetail accountLocked(OffsetDateTime lockedUntil) {
        return ErrorDetail.builder()
                .code(ErrorCode.AUTH_LOCKED.name())
                .message(ErrorCode.AUTH_LOCKED.getMessage())
                .lockedUntil(lockedUntil)
                .build();
    }
}

