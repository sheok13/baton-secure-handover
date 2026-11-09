package com.baton.ai.dto;

/**
 * 출처 한 건 (API 설계서 5.1 sources). DOCX는 page가 없어 null.
 * AI가 보낸 값은 그대로 믿지 않고 Backend가 DB 권한으로 재검증한 뒤 FE에 중계·저장한다.
 */
public record SourceDto(Long docId, String title, Integer page) {
}
