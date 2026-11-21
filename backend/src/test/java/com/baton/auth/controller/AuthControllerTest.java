package com.baton.auth.controller;

import com.baton.auth.dto.LoginRequest;
import com.baton.auth.dto.LoginResponse;
import com.baton.auth.dto.LoginResult;
import com.baton.auth.dto.MemberInfoResponse;
import com.baton.auth.service.AuthService;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import com.baton.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthController authController;

    private MemberInfoResponse memberInfo;

    @BeforeEach
    void setUp() {
        memberInfo = MemberInfoResponse.of("김영업", Department.SALES, Rank.STAFF);
    }

    @Test
    @DisplayName("로그인 성공 시 200 OK와 함께 HttpOnly baton_rt 쿠키 및 응답 본문이 반환된다")
    void login_success() {
        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("password123!")
                .build();

        LoginResponse loginResponse = LoginResponse.of("mock-token", 1800L, memberInfo);
        LoginResult loginResult = new LoginResult(loginResponse, "mock-refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResult);
        when(jwtProvider.getRefreshExpirationMs()).thenReturn(604800000L);

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNotNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("baton_rt=mock-refresh-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("HttpOnly");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Path=/api/auth");
    }

    @Test
    @DisplayName("로그아웃 호출 시 204 No Content와 함께 baton_rt 만료(Max-Age=0) 쿠키가 전달된다")
    void logout_success() {
        ResponseEntity<Void> response = authController.logout("mock-refresh-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("baton_rt=");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        verify(authService).logout("mock-refresh-token");
    }
}

