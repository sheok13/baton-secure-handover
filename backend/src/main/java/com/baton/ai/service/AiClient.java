package com.baton.ai.service;

import com.baton.ai.dto.AiChatEvent;
import com.baton.ai.dto.AiChatRequest;
import com.baton.ai.dto.AiIngestResponse;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import reactor.core.publisher.Flux;

/**
 * Backend → AI Module 내부 호출 (API 설계서 6장).
 * 구현체: WebClientAiClient(기본), MockAiClient(ai.mock-enabled=true, 로컬 확인용).
 */
public interface AiClient {

    /** 문서 수집. 실패하면 AiClientException */
    AiIngestResponse ingest(long docId, String title, Department department, Rank minRank,
                            FileType fileType, byte[] content);

    /** 수집 실패 시 잔여 청크 정리. 실패해도 예외를 던지지 않는다 (best effort) */
    void deleteChunks(long docId);

    /** 권한 필터 RAG 답변 스트림 (token → sources → done, 실패 시 error). 연결 실패는 Flux 오류(AiClientException) */
    Flux<AiChatEvent> streamChat(AiChatRequest request);
}
