package com.baton.ai.dto;

/** POST /internal/ingest 응답 (API 설계서 6.1, ai-module schemas.IngestResponse) */
public record AiIngestResponse(Long docId, int chunkCount, int maskedCount) {
}
