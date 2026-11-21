package com.baton.common.exception;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.BusinessException;
import lombok.Getter;

@Getter
public class LoginFailedException extends BusinessException {

    private final int remainingAttempts;

    public LoginFailedException(int remainingAttempts) {
        super(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        this.remainingAttempts = remainingAttempts;
    }

    public LoginFailedException(int remainingAttempts, String message) {
        super(AuthErrorCode.AUTH_INVALID_CREDENTIALS, message);
        this.remainingAttempts = remainingAttempts;
    }
}
