package com.baton.ai.service;

import com.baton.ai.dto.AiIngestResponse;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;

/**
 * Backend → AI Module 내부 호출 (API 설계서 6장).
 * 구현체: WebClientAiClient(기본), MockAiClient(ai.mock-enabled=true, 로컬 확인용).
 * 5단계에서 질의 SSE 메서드를 추가한다.
 */
public interface AiClient {

    /** 문서 수집. 실패하면 AiClientException */
    AiIngestResponse ingest(long docId, String title, Department department, Rank minRank,
                            FileType fileType, byte[] content);

    /** 수집 실패 시 잔여 청크 정리. 실패해도 예외를 던지지 않는다 (best effort) */
    void deleteChunks(long docId);
}
