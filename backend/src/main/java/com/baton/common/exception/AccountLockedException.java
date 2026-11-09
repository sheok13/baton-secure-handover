package com.baton.common.exception;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class AccountLockedException extends BusinessException {

    private final OffsetDateTime lockedUntil;

    public AccountLockedException(OffsetDateTime lockedUntil) {
        super(ErrorCode.AUTH_LOCKED);
        this.lockedUntil = lockedUntil;
    }

    public AccountLockedException(OffsetDateTime lockedUntil, String message) {
        super(ErrorCode.AUTH_LOCKED, message);
        this.lockedUntil = lockedUntil;
    }
}

