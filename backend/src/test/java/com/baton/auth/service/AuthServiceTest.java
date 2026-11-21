package com.baton.auth.service;

import com.baton.auth.dto.LoginRequest;
import com.baton.auth.dto.LoginResult;
import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import com.baton.member.repository.MemberRepository;
import com.baton.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .email("sales.staff@baton.local")
                .passwordHash("hashed-password")
                .name("김영업")
                .department(Department.SALES)
                .jobRank(Rank.STAFF)
                .failedLoginCount(0)
                .build();
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 회원 정보가 정상 반환된다")
    void login_success() {
        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("password123!")
                .build();

        when(memberRepository.findByEmail("sales.staff@baton.local")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123!", "hashed-password")).thenReturn(true);
        when(jwtProvider.createAccessToken(member.getUuid())).thenReturn("mock-access-token");
        when(jwtProvider.getAccessExpirationSeconds()).thenReturn(1800L);
        when(jwtProvider.createRefreshToken()).thenReturn("mock-refresh-token");
        when(jwtProvider.getRefreshExpirationMs()).thenReturn(604800000L);

        LoginResult result = authService.login(request);

        assertThat(result.response().getAccessToken()).isEqualTo("mock-access-token");
        assertThat(result.response().getExpiresIn()).isEqualTo(1800L);
        assertThat(result.response().getMember().getName()).isEqualTo("김영업");
        assertThat(result.response().getMember().getRank()).isEqualTo(Rank.STAFF);
        assertThat(result.refreshToken()).isEqualTo("mock-refresh-token");
        assertThat(member.getFailedLoginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 남은 횟수와 함께 LoginFailedException 발생")
    void login_passwordMismatch() {
        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("wrong-password")
                .build();

        when(memberRepository.findByEmail("sales.staff@baton.local")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(LoginFailedException.class)
                .extracting(e -> ((LoginFailedException) e).getRemainingAttempts())
                .isEqualTo(4);

        assertThat(member.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("연속 5회 실패 시 AccountLockedException 발생 및 15분 잠금 설정")
    void login_5timesFailed_locksAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("wrong-password")
                .build();

        // 이미 4회 실패 상태
        for (int i = 0; i < 4; i++) {
            member.recordLoginFailure(5, 15, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        }

        when(memberRepository.findByEmail("sales.staff@baton.local")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);

        assertThat(member.getFailedLoginCount()).isEqualTo(5);
        assertThat(member.getLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("계정이 이미 잠긴 상태면 비밀번호 검사 없이 즉시 AccountLockedException 발생")
    void login_alreadyLocked() {
        LocalDateTime lockedUntil = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(10);
        Member lockedMember = Member.builder()
                .email("sales.staff@baton.local")
                .failedLoginCount(5)
                .lockedUntil(lockedUntil)
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("sales.staff@baton.local")
                .password("any-password")
                .build();

        when(memberRepository.findByEmail("sales.staff@baton.local")).thenReturn(Optional.of(lockedMember));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 로그인 시도 시 더미 연산 후 4회 남음으로 LoginFailedException 발생 (계정 열거 방어)")
    void login_userEnumerationDefense() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@baton.local")
                .password("password123!")
                .build();

        when(memberRepository.findByEmail("unknown@baton.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(LoginFailedException.class)
                .extracting(e -> ((LoginFailedException) e).getRemainingAttempts())
                .isEqualTo(4);

        // 타이밍 어택 방어용 더미 비교가 호출되었는지 확인
        verify(passwordEncoder).matches(eq("password123!"), anyString());
    }

    @Test
    @DisplayName("이메일 마스킹 유틸 검증")
    void maskEmail() {
        assertThat(AuthService.maskEmail("sales.staff@baton.local")).isEqualTo("s***@baton.local");
        assertThat(AuthService.maskEmail("a@baton.local")).isEqualTo("a***@baton.local");
    }
}

