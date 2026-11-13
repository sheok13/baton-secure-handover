package com.baton.auth.controller;

import com.baton.auth.dto.LoginRequest;
import com.baton.auth.dto.LoginResponse;
import com.baton.auth.dto.LoginResult;
import com.baton.auth.dto.MemberInfoResponse;
import com.baton.auth.exception.AuthErrorCode;
import com.baton.auth.service.AuthService;
import com.baton.common.ApiResponse;
import com.baton.common.BusinessException;
import com.baton.security.AuthMember;
import com.baton.security.jwt.JwtProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러 (A 소유, API 설계서 3장).
 * 
 * - POST /api/auth/login : 로그인 (200 + Set-Cookie baton_rt)
 * - POST /api/auth/refresh : 토큰 재발급 (200 + 새 Set-Cookie)
 * - POST /api/auth/logout : 로그아웃 (204 + 쿠키 삭제)
 * - GET  /api/auth/me : 현재 로그인 사용자 정보
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "baton_rt";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @Value("${jwt.cookie.secure:false}")
    private boolean secureCookie;

    /**
     * 로그인 (POST /api/auth/login)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                jwtProvider.getRefreshExpirationMs() / 1000
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.response()));
    }

    /**
     * Access/Refresh 토큰 재발급 (POST /api/auth/refresh)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID);
        }

        LoginResult result = authService.reissue(refreshToken);

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                jwtProvider.getRefreshExpirationMs() / 1000
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.response()));
    }

    /**
     * 로그아웃 (POST /api/auth/logout, 204 No Content)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        ResponseCookie deleteCookie = createDeleteRefreshTokenCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    /**
     * 내 정보 조회 (GET /api/auth/me)
     */
    @GetMapping("/me")
    public ApiResponse<MemberInfoResponse> me(@AuthenticationPrincipal AuthMember me) {
        if (me == null) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }
        return ApiResponse.success(authService.getMe(me.uuid()));
    }

    /**
     * Refresh Token용 HttpOnly 쿠키 생성 (SameSite=Strict, Path=/api/auth)
     */
    private ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * Refresh Token 쿠키 삭제용 만료 쿠키 생성 (Max-Age=0)
     */
    private ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}

