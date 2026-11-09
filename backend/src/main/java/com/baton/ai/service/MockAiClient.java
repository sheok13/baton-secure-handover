package com.baton.ai.service;

import com.baton.ai.dto.AiIngestResponse;
import com.baton.ai.exception.AiClientException;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 확인용 가짜 AI (ai.mock-enabled=true일 때만 등록). 기본값은 꺼짐 → 배포 환경에서는 WebClientAiClient만 쓴다.
 * - 제목에 "[FAIL]"이 들어 있으면 수집 실패를 흉내 낸다 (FAILED 처리·청크 정리 흐름 확인용)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock-enabled", havingValue = "true")
public class MockAiClient implements AiClient {

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
}
