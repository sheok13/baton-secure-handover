package com.baton.security.jwt;

import com.baton.member.entity.Member;
import com.baton.member.repository.MemberRepository;
import com.baton.security.AuthMember;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JWT 인증 필터 (A 소유).
 * 
 * - 모든 요청에서 Authorization: Bearer <token> 헤더 추출
 * - 토큰 유효성 검증 후 sub(uuid)로 DB 최신 Member 조회
 * - AuthMember(id, uuid, department, rank)를 생성해 SecurityContext에 주입
 *   -> 컨트롤러에서 @AuthenticationPrincipal AuthMember me 로 주입받아 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            String uuid = jwtProvider.getUuid(token);
            Optional<Member> memberOpt = memberRepository.findByUuid(uuid);

            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();

                // B 파트 컨트롤러가 주입받는 AuthMember record 생성
                AuthMember authMember = new AuthMember(
                        member.getId(),
                        member.getUuid(),
                        member.getDepartment(),
                        member.getJobRank()
                );

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + member.getJobRank().name())
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authMember,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("토큰의 uuid에 해당하는 회원을 찾을 수 없습니다: uuid={}", uuid);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}

