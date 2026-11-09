package com.baton.auth.dto;

/**
 * 로그인 처리 결과 (컨트롤러 전달용).
 * 응답 본문용 LoginResponse 와 HttpOnly 쿠키용 refreshToken 원문을 함께 보관한다.
 */
public record LoginResult(
        LoginResponse response,
        String refreshToken
) {
}

