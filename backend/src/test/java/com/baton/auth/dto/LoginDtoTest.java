package com.baton.auth.dto;

import com.baton.common.ApiResponse;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginDtoTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = JsonMapper.builder().build();
    }

    @Test
    @DisplayName("LoginRequest 정상 검증")
    void loginRequest_valid() {
        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("password123!")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("LoginRequest 잘못된 이메일 또는 빈 비밀번호 유효성 검사 실패")
    void loginRequest_invalid() {
        LoginRequest request = LoginRequest.builder()
                .email("not-an-email")
                .password("")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("로그인 실패 시 ApiResponse에 remainingAttempts가 포함되어 직렬화된다")
    void loginFailure_serialization() throws Exception {
        ApiResponse<Void> response = ApiResponse.loginFailure(3);
        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"AUTH_INVALID_CREDENTIALS\"");
        assertThat(json).contains("\"remainingAttempts\":3");
        assertThat(json).doesNotContain("\"lockedUntil\"");
    }

    @Test
    @DisplayName("계정 잠금 시 ApiResponse에 lockedUntil 타임존 포함 ISO 8601 문자열이 직렬화된다")
    void accountLocked_serialization() throws Exception {
        OffsetDateTime lockedUntil = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(9));
        ApiResponse<Void> response = ApiResponse.accountLocked(lockedUntil);
        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"AUTH_LOCKED\"");
        assertThat(json).contains("\"lockedUntil\":\"2026-11-09T10:00:00+09:00\"");
        assertThat(json).doesNotContain("\"remainingAttempts\"");
    }

    @Test
    @DisplayName("LoginResponse 직렬화 시 member의 직급이 rank 키로 직렬화된다")
    void loginResponse_serialization() throws Exception {
        MemberInfoResponse member = MemberInfoResponse.of("김영업", Department.SALES, Rank.STAFF);
        LoginResponse response = LoginResponse.of("token123", 1800, member);

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"rank\":\"STAFF\"");
        assertThat(json).contains("\"department\":\"SALES\"");
        assertThat(json).contains("\"accessToken\":\"token123\"");
        assertThat(json).contains("\"expiresIn\":1800");
    }
}
