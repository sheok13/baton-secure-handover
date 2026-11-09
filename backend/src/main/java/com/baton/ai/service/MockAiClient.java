package com.baton.ai.service;

import com.baton.ai.dto.AiChatEvent;
import com.baton.ai.dto.AiChatRequest;
import com.baton.ai.dto.AiIngestResponse;
import com.baton.ai.exception.AiClientException;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 로컬 확인용 가짜 AI (ai.mock-enabled=true일 때만 등록). 기본값은 꺼짐 → 배포 환경에서는 WebClientAiClient만 쓴다.
 * 수집: 제목에 "[FAIL]" → 수집 실패
 * 채팅: 질문에 "[ERROR]" → 중간에 error 이벤트 / "[CUT]" → done 없이 끊김 / "[SLOW]" → 첫 토큰 35초 지연(타임아웃 확인)
 * 채팅 출처는 일부러 권한 밖 문서(5번 영업 팀장 공개, 7번 개발 문서)를 섞어 보낸다 → Backend 재검증으로 걸러지는지 확인
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock-enabled", havingValue = "true")
public class MockAiClient implements AiClient {

    private static final String MOCK_SOURCES = """
            [{"docId":3,"title":"거래처 관리 매뉴얼","page":null},\
            {"docId":5,"title":"거래처 계약 단가표","page":null},\
            {"docId":7,"title":"개발 환경 세팅 가이드","page":null}]""";

    @PostConstruct
    void warn() {
        log.warn("ai.mock-enabled=true — AI 모듈 대신 가짜 응답을 사용합니다. 배포 환경에서는 켜지 마세요.");
    }

    @Override
    public AiIngestResponse ingest(long docId, String title, Department department, Rank minRank,
                                   FileType fileType, byte[] content) {
        if (title != null && title.contains("[FAIL]")) {
            throw new AiClientException("mock: 수집 실패 시뮬레이션 docId=" + docId);
        }
        int chunks = Math.max(1, content.length / 2000);
        log.info("mock ingest docId={} title={} {}·{} chunks={}", docId, title, department, minRank, chunks);
        return new AiIngestResponse(docId, chunks, 0);
    }

    @Override
    public void deleteChunks(long docId) {
        log.info("mock deleteChunks docId={}", docId);
    }

    @Override
    public Flux<AiChatEvent> streamChat(AiChatRequest request) {
        String q = request.question();
        log.info("mock chat filter={} history={}", request.filter(), request.history().size());

        if (q.contains("[ERROR]")) {
            return Flux.just(token("답변을 만들다가 "),
                    new AiChatEvent(AiChatEvent.ERROR, "{\"code\":\"UPSTREAM_FAILED\",\"message\":\"mock\"}"));
        }
        if (q.contains("[CUT]")) {
            return Flux.just(token("중간에 끊기는 답변"));
        }

        List<String> parts = List.of(
                "가짜 AI 답변입니다. ",
                "질문 \"" + q + "\"을 받았고, ",
                "권한 필터는 부서 " + request.filter().departments()
                        + " · 직급 " + request.filter().ranks() + " 입니다.");
        Flux<AiChatEvent> stream = Flux.concat(
                Flux.fromIterable(parts).map(this::token).delayElements(Duration.ofMillis(200)),
                Flux.just(new AiChatEvent(AiChatEvent.SOURCES, MOCK_SOURCES),
                        new AiChatEvent(AiChatEvent.DONE, "{}")));

        return q.contains("[SLOW]") ? stream.delaySubscription(Duration.ofSeconds(35)) : stream;
    }

    private AiChatEvent token(String text) {
        return new AiChatEvent(AiChatEvent.TOKEN, AiJson.MAPPER.writeValueAsString(Map.of("text", text)));
    }
}
