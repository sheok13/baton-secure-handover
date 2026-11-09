package com.baton.security.config;

import com.baton.auth.exception.AuthErrorCode;
import com.baton.common.ApiResponse;
import com.baton.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 * 
 * - JWT 기반 무상태(Stateless) REST API 아키텍처
 * - CSRF 비활성화 (Bearer 토큰 기반), FormLogin 및 HttpBasic 비활성화
 * - 보안 응답 헤더(XSS, Clickjacking 방어: CSP, HSTS, X-Frame-Options, X-Content-Type-Options)
 * - 401(인증 실패), 403(인가 실패) 시 ApiResponse 규격 JSON 응답 및 [AUDIT] 감사 로깅
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API 서버이므로 CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 세션을 생성하거나 사용하지 않음 (STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 브라우저 기본 폼 로그인 및 HTTP Basic 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 보안 헤더 하드닝 (XSS, Sniffing, Clickjacking 방어)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )

                // 경로별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // SSE 비동기 디스패치 및 에러 포워딩 허용
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        // 로그인, 리프레시 토큰 재발급, 헬스체크는 인증 없이 접근 허용
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // 헬스체크 외 모든 Actuator 엔드포인트 차단
                        .requestMatchers("/actuator/**").denyAll()

                        // 그 외 모든 API 요청은 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 그 외 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 필터 단계 예외 처리 (공통 ApiResponse JSON 규격 + [AUDIT] 로깅)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Requested-With"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 인증 실패 (401 Unauthorized) 핸들러
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("[AUDIT] 인증 실패(401) - {} {} from {}: {}",
                    request.getMethod(), request.getRequestURI(), getClientIp(request), authException.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.error(AuthErrorCode.AUTH_TOKEN_INVALID);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    /**
     * 접근 거부 (403 Forbidden) 핸들러
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("[AUDIT] 접근 거부(403) - {} {} from {}: {}",
                    request.getMethod(), request.getRequestURI(), getClientIp(request), accessDeniedException.getMessage());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorCode forbiddenError = new ErrorCode() {
                @Override
                public HttpStatus getStatus() { return HttpStatus.FORBIDDEN; }
                @Override
                public String getCode() { return "FORBIDDEN"; }
                @Override
                public String getMessage() { return "접근 권한이 없습니다."; }
            };

            ApiResponse<Void> errorResponse = ApiResponse.error(forbiddenError);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    /**
     * 클라이언트 IP 추출 (프록시 X-Forwarded-For 지원)
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
