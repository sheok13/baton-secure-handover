package com.baton.common;

import com.baton.common.dto.ErrorDetail;
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
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private ErrorDetail error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetail.of(errorCode))
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetail.of(errorCode, customMessage))
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorDetail errorDetail) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorDetail)
                .build();
    }

    public static <T> ApiResponse<T> loginFailure(int remainingAttempts) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetail.loginFailure(remainingAttempts))
                .build();
    }

    public static <T> ApiResponse<T> accountLocked(OffsetDateTime lockedUntil) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetail.accountLocked(lockedUntil))
                .build();
    }
}

