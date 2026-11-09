package com.baton.chat.dto;

import com.baton.chat.entity.ChatSession;
import com.baton.document.dto.KstTime;

/** GET /chat/sessions 목록 항목 (API 설계서 5.0) */
public record ChatSessionResponse(Long id, String title, String updatedAt) {

    public static ChatSessionResponse from(ChatSession s) {
        return new ChatSessionResponse(s.getId(), s.getTitle(), KstTime.format(s.getUpdatedAt()));
    }
}
