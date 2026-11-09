package com.baton.common.exception;

import lombok.Getter;

@Getter
public class LoginFailedException extends BusinessException {

    private final int remainingAttempts;

    public LoginFailedException(int remainingAttempts) {
        super(ErrorCode.AUTH_INVALID_CREDENTIALS);
        this.remainingAttempts = remainingAttempts;
    }

    public LoginFailedException(int remainingAttempts, String message) {
        super(ErrorCode.AUTH_INVALID_CREDENTIALS, message);
        this.remainingAttempts = remainingAttempts;
    }
}

