package com.baton.common.exception;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.BusinessException;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class AccountLockedException extends BusinessException {

    private final OffsetDateTime lockedUntil;

    public AccountLockedException(OffsetDateTime lockedUntil) {
        super(AuthErrorCode.AUTH_LOCKED);
        this.lockedUntil = lockedUntil;
    }

    public AccountLockedException(OffsetDateTime lockedUntil, String message) {
        super(AuthErrorCode.AUTH_LOCKED, message);
        this.lockedUntil = lockedUntil;
    }
}
