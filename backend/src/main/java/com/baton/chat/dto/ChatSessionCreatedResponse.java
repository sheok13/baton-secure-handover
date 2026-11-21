package com.baton.chat.dto;

import com.baton.chat.entity.ChatSession;
import com.baton.document.dto.KstTime;

/** POST /chat/sessions 응답 201 (API 설계서 5.0) */
public record ChatSessionCreatedResponse(Long id, String title, String createdAt) {

    public static ChatSessionCreatedResponse from(ChatSession s) {
        return new ChatSessionCreatedResponse(s.getId(), s.getTitle(), KstTime.format(s.getCreatedAt()));
    }
}
