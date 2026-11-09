package com.baton.ai.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * AI Module 전용 WebClient (Backend 명세 6장).
 * 모든 /internal/* 요청에 X-Internal-Api-Key 자동 첨부, 연결 타임아웃 3초.
 * 응답 타임아웃은 호출마다 다르므로(수집 120초, 채팅 첫 토큰 30초) 각 호출에서 지정한다.
 */
@Slf4j
@Configuration
public class AiWebClientConfig {

    @Bean
    public WebClient aiWebClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.internal-api-key:}") String internalApiKey,
            @Value("${ai.connect-timeout-ms:3000}") int connectTimeoutMs) {
        if (internalApiKey.isBlank()) {
            log.warn("ai.internal-api-key(AI_INTERNAL_API_KEY)가 비어 있습니다. AI 모듈 호출은 401로 실패합니다.");
        }
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
