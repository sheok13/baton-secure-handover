package com.baton.ai.dto;

import com.baton.ai.service.AiFilter;
import com.baton.chat.entity.MessageRole;
import java.util.List;

/**
 * POST /internal/chat 요청 (API 설계서 6.2, ai-module schemas.ChatRequest).
 * filter는 DB 회원 정보로만 만든다. userId·부서·직급 원본 값은 보내지 않는다.
 */
public record AiChatRequest(String question, AiFilter filter, List<HistoryItem> history) {

    public record HistoryItem(MessageRole role, String content) {
    }
}
