package com.baton.auth.service;

import com.baton.auth.dto.LoginRequest;
import com.baton.auth.dto.LoginResponse;
import com.baton.auth.dto.LoginResult;
import com.baton.auth.dto.MemberInfoResponse;
import com.baton.common.BusinessException;
import com.baton.common.exception.AccountLockedException;
import com.baton.common.exception.LoginFailedException;
import com.baton.member.entity.Member;
import com.baton.member.repository.MemberRepository;
import com.baton.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 인증 및 계정 로그인 서비스 (A 소유).
 * 
 * - 연속 5회 로그인 실패 시 15분간 계정 잠금 (Brute Force 방어)
 * - 미존재 이메일 요청 시 더미 BCrypt 연산 및 가상 횟수 반환 (계정 열거 및 타이밍 어택 방어)
 * - 잠금 시 ISO 8601 타임존 포함 lockedUntil 반환 (423 AUTH_LOCKED)
 * - 로그인 성공 시 실패 카운트 초기화 및 Access/Refresh 토큰 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    // 계정 열거 방어용 더미 해시 (BCrypt 계산 시간 동기화)
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그인 인증 처리
     * 
     * 비밀번호 불일치(LoginFailedException) 또는 계정 잠금(AccountLockedException) 발생 시에도
     * failed_login_count 및 locked_until 변경 사항이 DB에 정상 커밋되도록 noRollbackFor 설정.
     */
    @Transactional(noRollbackFor = {LoginFailedException.class, AccountLockedException.class})
    public LoginResult login(LoginRequest request) {
        String email = request.getEmail().strip();
        String maskedEmail = maskEmail(email);

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        // 1. 존재하지 않는 계정: 계정 열거(User Enumeration) 및 타이밍 어택 방어
        if (memberOpt.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            log.warn("[AUDIT] LOGIN_FAILED email={} reason=MEMBER_NOT_FOUND", maskedEmail);
            // 가입 계정의 첫 실패와 동일하게 4회 남음으로 응답
            throw new LoginFailedException(MAX_ATTEMPTS - 1);
        }

        Member member = memberOpt.get();
        LocalDateTime now = LocalDateTime.now(KST);

        // 2. 계정 잠금 상태 확인
        if (member.isLocked(now)) {
            OffsetDateTime lockedUntilKst = member.getLockedUntil().atZone(KST).toOffsetDateTime();
            log.warn("[AUDIT] LOGIN_BLOCKED_LOCKED email={} lockedUntil={}", maskedEmail, lockedUntilKst);
            throw new AccountLockedException(lockedUntilKst);
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            int remainingAttempts = member.recordLoginFailure(MAX_ATTEMPTS, LOCK_DURATION_MINUTES, now);
            memberRepository.save(member);

            if (remainingAttempts == 0) {
                OffsetDateTime lockedUntilKst = member.getLockedUntil().atZone(KST).toOffsetDateTime();
                log.warn("[AUDIT] ACCOUNT_LOCKED email={} lockedUntil={}", maskedEmail, lockedUntilKst);
                throw new AccountLockedException(lockedUntilKst);
            }

            log.warn("[AUDIT] LOGIN_FAILED email={} remainingAttempts={}", maskedEmail, remainingAttempts);
            throw new LoginFailedException(remainingAttempts);
        }

        // 4. 로그인 성공: 실패 횟수 초기화 및 토큰 발급
        member.resetLoginFailure();

        String accessToken = jwtProvider.createAccessToken(member.getUuid());
        long expiresIn = jwtProvider.getAccessExpirationSeconds();

        String refreshToken = jwtProvider.createRefreshToken();
        LocalDateTime refreshExpiresAt = now.plusSeconds(jwtProvider.getRefreshExpirationMs() / 1000);
        refreshTokenService.saveRefreshToken(member, refreshToken, refreshExpiresAt);

        log.info("[AUDIT] LOGIN_SUCCESS memberUuid={} email={}", member.getUuid(), maskedEmail);

        MemberInfoResponse memberInfo = MemberInfoResponse.of(member.getName(), member.getDepartment(), member.getJobRank());
        LoginResponse response = LoginResponse.of(accessToken, expiresIn, memberInfo);

        return new LoginResult(response, refreshToken);
    }

    /**
     * 토큰 재발급 (POST /api/auth/refresh)
     * Refresh Token 쿠키를 읽어 새 Access Token 및 새 Refresh Token 발급 (Rotation)
     * 재사용 감지 시 일괄 폐기 트랜잭션이 롤백되지 않도록 noRollbackFor 적용.
     */
    @Transactional(noRollbackFor = {BusinessException.class})
    public LoginResult reissue(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new com.baton.common.BusinessException(com.baton.auth.exception.AuthErrorCode.AUTH_REFRESH_INVALID);
        }

        String newRefreshToken = jwtProvider.createRefreshToken();
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDateTime newExpiresAt = now.plusSeconds(jwtProvider.getRefreshExpirationMs() / 1000);

        Member member = refreshTokenService.validateAndRotate(rawRefreshToken, newRefreshToken, newExpiresAt);

        String newAccessToken = jwtProvider.createAccessToken(member.getUuid());
        long expiresIn = jwtProvider.getAccessExpirationSeconds();

        MemberInfoResponse memberInfo = MemberInfoResponse.of(member.getName(), member.getDepartment(), member.getJobRank());
        LoginResponse response = LoginResponse.of(newAccessToken, expiresIn, memberInfo);

        return new LoginResult(response, newRefreshToken);
    }

    /**
     * 내 정보 조회 (GET /api/auth/me)
     */
    public MemberInfoResponse getMe(String uuid) {
        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return MemberInfoResponse.of(member.getName(), member.getDepartment(), member.getJobRank());
    }

    /**
     * 로그아웃 (Refresh 토큰 폐기)
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    /**
     * 이메일 마스킹 (감사 로그용: sales.staff@baton.local -> s***@baton.local)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (name.length() <= 1) {
            return name + "***" + domain;
        }
        return name.charAt(0) + "***" + domain;
    }
}

