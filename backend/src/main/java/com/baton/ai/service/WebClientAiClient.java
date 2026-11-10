package com.baton.ai.service;

import com.baton.ai.dto.AiChatEvent;
import com.baton.ai.dto.AiChatRequest;
import com.baton.ai.dto.AiIngestResponse;
import com.baton.ai.exception.AiClientException;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

/** 실제 AI Module 호출 (기본 구현체). ai.mock-enabled=true일 때만 MockAiClient로 바뀐다 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock-enabled", havingValue = "false", matchIfMissing = true)
public class WebClientAiClient implements AiClient {

    private static final Duration DELETE_TIMEOUT = Duration.ofSeconds(10);
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient aiWebClient;
    private final Duration ingestTimeout;

    public WebClientAiClient(@Qualifier("aiWebClient") WebClient aiWebClient,
                             @Value("${ai.ingest-timeout-ms:120000}") long ingestTimeoutMs) {
        this.aiWebClient = aiWebClient;
        this.ingestTimeout = Duration.ofMillis(ingestTimeoutMs);
    }

    @Override
    public AiIngestResponse ingest(long docId, String title, Department department, Rank minRank,
                                   FileType fileType, byte[] content) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        // AI는 파일명의 확장자로 PDF/DOCX 로더를 고른다. 원본 파일명(한글·특수문자)은 보내지 않고 ASCII 이름만 쓴다
        body.part("file", new ByteArrayResource(content))
                .filename("doc-" + docId + "." + fileType.extension())
                .contentType(MediaType.parseMediaType(fileType.mimeType()));
        body.part("docId", String.valueOf(docId));
        body.part("title", title);
        body.part("department", department.name());
        body.part("minRank", minRank.name());

        try {
            AiIngestResponse res = aiWebClient.post()
                    .uri("/internal/ingest")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body.build()))
                    .retrieve()
                    .bodyToMono(AiIngestResponse.class)
                    .timeout(ingestTimeout)
                    .block();
            if (res == null || res.docId() == null || res.docId() != docId) {
                throw new AiClientException("AI 수집 응답의 docId가 요청과 다릅니다. 요청=" + docId);
            }
            return res;
        } catch (AiClientException e) {
            throw e;
        } catch (WebClientResponseException e) {
            // AI 오류 본문(code·message)은 서버 로그에만 남기고 사용자에게는 AI_INGEST_FAILED로 응답한다
            throw new AiClientException("AI 수집 오류 status=" + e.getStatusCode().value()
                    + " body=" + abbreviate(e.getResponseBodyAsString()), e);
        } catch (RuntimeException e) {
            throw new AiClientException("AI 수집 호출 실패: " + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void deleteChunks(long docId) {
        try {
            aiWebClient.delete()
                    .uri("/internal/documents/{docId}", docId)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(DELETE_TIMEOUT)
                    .block();
            log.info("AI 잔여 청크 정리 완료 docId={}", docId);
        } catch (RuntimeException e) {
            log.warn("AI 잔여 청크 정리 실패 docId={} reason={}", docId, e.getClass().getSimpleName());
        }
    }

    /** 타임아웃(첫 토큰 30초·전체 120초)은 ChatStreamService에서 공통으로 건다 (가짜 AI에도 같은 규칙 적용) */
    @Override
    public Flux<AiChatEvent> streamChat(AiChatRequest request) {
        return aiWebClient.post()
                .uri("/internal/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(SSE_TYPE)
                .filter(sse -> sse.event() != null)
                .map(sse -> new AiChatEvent(sse.event(), sse.data()))
                .onErrorMap(e -> !(e instanceof AiClientException), e -> {
                    if (e instanceof WebClientResponseException w) {
                        return new AiClientException("AI 채팅 오류 status=" + w.getStatusCode().value()
                                + " body=" + abbreviate(w.getResponseBodyAsString()), e);
                    }
                    return new AiClientException("AI 채팅 호출 실패: " + e.getClass().getSimpleName(), e);
                });
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
