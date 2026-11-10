package com.baton.security.audit;

import com.baton.auth.dto.LoginRequest;
import com.baton.auth.entity.RefreshToken;
import com.baton.auth.exception.AuthErrorCode;
import com.baton.auth.repository.RefreshTokenRepository;
import com.baton.auth.service.AuthService;
import com.baton.auth.service.RefreshTokenService;
import com.baton.common.BusinessException;
import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import com.baton.member.repository.MemberRepository;
import com.baton.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 🛡️ 4일차 보안 점검(Security Audit) 시나리오 검증 테스트
 * 
 * - 취약점 1: 무차별 대입 공격(Brute-Force) 및 계정 잠금 한계 테스트
 * - 취약점 2: 사용자 계정 열거(User Enumeration) 및 Timing Attack 방어
 * - 취약점 3: JWT 서명 변조(Signature Tampering) 및 조작 토큰 차단
 * - 취약점 4: Refresh Token 탈취 재사용(Replay Attack) 감지 및 일괄 폐기
 */
@ExtendWith(MockitoExtension.class)
class SecurityAuditTest {

    @Nested
    @DisplayName("[점검 1] 무차별 대입 공격(Brute-Force) 및 계정 잠금 검증")
    class BruteForceAudit {

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

        private Member targetMember;

        @BeforeEach
        void setUp() {
            targetMember = Member.builder()
                    .id(1L)
                    .uuid(UUID.randomUUID().toString())
                    .email("sales.staff@baton.local")
                    .passwordHash("$2a$10$hashedPasswordSample...")
                    .name("김영업")
                    .department(Department.SALES)
                    .jobRank(Rank.STAFF)
                    .failedLoginCount(0)
                    .build();
        }

        @Test
        @DisplayName("공격자가 5회 연속 잘못된 비밀번호 입력 시 -> 계정이 즉시 15분간 잠기고(423) lockedUntil 반환")
        void bruteForceLockoutAfter5Failures() {
            when(memberRepository.findByEmail(targetMember.getEmail())).thenReturn(Optional.of(targetMember));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            LoginRequest attackRequest = new LoginRequest(targetMember.getEmail(), "wrong-password");

            // 1~4회 실패: 남은 시도 횟수가 차례로 감소
            for (int attempt = 1; attempt <= 4; attempt++) {
                final int currentAttempt = attempt;
                assertThatThrownBy(() -> authService.login(attackRequest))
                        .isInstanceOf(LoginFailedException.class)
                        .satisfies(ex -> {
                            LoginFailedException lfe = (LoginFailedException) ex;
                            assertThat(lfe.getRemainingAttempts()).isEqualTo(5 - currentAttempt);
                        });
            }

            // 5회 실패: 계정이 즉시 잠기고 AccountLockedException 발생
            assertThatThrownBy(() -> authService.login(attackRequest))
                    .isInstanceOf(AccountLockedException.class)
                    .satisfies(ex -> {
                        AccountLockedException ale = (AccountLockedException) ex;
                        assertThat(ale.getLockedUntil()).isNotNull();
                        System.out.println("[AUDIT VERIFIED] 5회 실패로 계정 잠금 성공 - lockedUntil: " + ale.getLockedUntil());
                    });

            assertThat(targetMember.getFailedLoginCount()).isEqualTo(5);
            assertThat(targetMember.getLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("잠긴 계정은 올바른 비밀번호를 입력해도 잠금 시간이 끝나기 전까지 로그인 차단")
        void lockedAccountRejectsEvenCorrectPassword() {
            // 이미 5회 실패로 잠긴 상태 (+10분 남음)
            targetMember.recordLoginFailure(5, 15, LocalDateTime.now().minusMinutes(5));
            when(memberRepository.findByEmail(targetMember.getEmail())).thenReturn(Optional.of(targetMember));

            LoginRequest correctRequest = new LoginRequest(targetMember.getEmail(), "baton1234!");

            assertThatThrownBy(() -> authService.login(correctRequest))
                    .isInstanceOf(AccountLockedException.class);

            // 비밀번호 대조 자체를 거치지 않고 바로 차단됨을 검증
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            System.out.println("[AUDIT VERIFIED] 잠긴 계정은 올바른 비밀번호라도 사전 차단됨 확인");
        }
    }

    @Nested
    @DisplayName("[점검 2] 계정 열거(User Enumeration) 및 Timing Attack 방어 검증")
    class UserEnumerationAudit {

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

        @Test
        @DisplayName("존재하지 않는 계정으로 로그인 시도 시 -> 더미 해시 연산을 거치며 남은 시도 4회로 일관되게 응답")
        void nonexistentAccountReturnsUniform4AttemptsAndRunsDummyHash() {
            String fakeEmail = "attacker-target@baton.local";
            when(memberRepository.findByEmail(fakeEmail)).thenReturn(Optional.empty());

            LoginRequest attackRequest = new LoginRequest(fakeEmail, "any-password");

            assertThatThrownBy(() -> authService.login(attackRequest))
                    .isInstanceOf(LoginFailedException.class)
                    .satisfies(ex -> {
                        LoginFailedException lfe = (LoginFailedException) ex;
                        // 일반 계정의 첫 1회 실패와 완전히 동일하게 남은 횟수 4회 응답
                        assertThat(lfe.getRemainingAttempts()).isEqualTo(4);
                    });

            // 타이밍 어택 방어를 위해 DUMMY 해시와의 비교가 반드시 1회 수행되었는지 검증
            verify(passwordEncoder, times(1)).matches(eq("any-password"), anyString());
            System.out.println("[AUDIT VERIFIED] 계정 열거 방어 더미 해시 연산 및 4회 잔여 응답 확인 완료");
        }
    }

    @Nested
    @DisplayName("[점검 3] JWT 서명 변조 및 악의적 토큰 차단 검증")
    class JwtTamperingAudit {

        private JwtProvider jwtProvider;
        private final String secret = "this-is-a-super-secret-key-that-is-at-least-32bytes-long!";

        @BeforeEach
        void setUp() {
            jwtProvider = new JwtProvider();
            ReflectionTestUtils.setField(jwtProvider, "secretKeyString", secret);
            ReflectionTestUtils.setField(jwtProvider, "accessExpirationMs", 1800000L);
            ReflectionTestUtils.setField(jwtProvider, "refreshExpirationMs", 604800000L);
            jwtProvider.init();
        }

        @Test
        @DisplayName("정상 발급된 토큰의 시그니처 1글자만 변조되어도 유효성 검증 실패(false)")
        void tamperedSignatureRejected() {
            String validToken = jwtProvider.createAccessToken(UUID.randomUUID().toString());
            assertThat(jwtProvider.validateToken(validToken)).isTrue();

            // 시그니처 끝자리 1문자 변조
            String tamperedToken = validToken.substring(0, validToken.length() - 1) + "X";

            assertThat(jwtProvider.validateToken(tamperedToken)).isFalse();
            System.out.println("[AUDIT VERIFIED] 변조된 JWT 시그니처 차단 확인 완료");
        }

        @Test
        @DisplayName("만료된 토큰(accessExpirationMs = -1)은 유효성 검증 실패(false)")
        void expiredTokenRejected() {
            JwtProvider expiredProvider = new JwtProvider();
            ReflectionTestUtils.setField(expiredProvider, "secretKeyString", secret);
            ReflectionTestUtils.setField(expiredProvider, "accessExpirationMs", -1000L); // 이미 만료
            expiredProvider.init();

            String expiredToken = expiredProvider.createAccessToken(UUID.randomUUID().toString());
            assertThat(expiredProvider.validateToken(expiredToken)).isFalse();
            System.out.println("[AUDIT VERIFIED] 만료된 토큰 차단 확인 완료");
        }
    }

    @Nested
    @DisplayName("[점검 4] Refresh Token 탈취 및 재사용 감지(Reuse Detection) 검증")
    class RefreshTokenReuseAudit {

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @InjectMocks
        private RefreshTokenService refreshTokenService;

        @Test
        @DisplayName("이미 폐기된(Revoked) Refresh Token으로 재발급 시도 시 -> 탈취로 판단하여 해당 회원의 모든 토큰 즉시 폐기")
        void reusedRevokedTokenTriggersFullRevocation() {
            Long memberId = 100L;
            String rawToken = "sample-raw-refresh-token-12345";
            RefreshToken revokedToken = RefreshToken.builder()
                    .id(1L)
                    .memberId(memberId)
                    .tokenHash("some-sha256-hash")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revokedAt(LocalDateTime.now().minusHours(1)) // 1시간 전에 이미 회전되어 폐기됨
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

            // 폐기된 토큰으로 재발급 요청 시 AUTH_REFRESH_INVALID 예외 발생
            assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_REFRESH_INVALID);
                    });

            // 🚨 핵심 방어: 해당 회원의 활성 토큰을 모두 강제 폐기(revokeAllByMemberId)했는지 검증
            verify(refreshTokenRepository, times(1)).revokeAllByMemberId(eq(memberId), any(LocalDateTime.class));
            System.out.println("[AUDIT VERIFIED] 폐기된 Refresh Token 재사용 공격 감지 -> 전체 토큰 일괄 폐기 확인 완료");
        }
    }
}

