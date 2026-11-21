package com.baton.security;

import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;

/**
 * SecurityContext에 저장되는 인증 사용자 정보 (A 소유).
 * JWT 필터가 토큰의 uuid로 회원을 조회하여 주입한다.
 * 컨트롤러에서 @AuthenticationPrincipal AuthMember me 로 주입받아 사용한다.
 */
public record AuthMember(
        Long id,
        String uuid,
        Department department,
        Rank rank
) {
}

