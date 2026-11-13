package com.baton.chat.dto;

import com.baton.chat.entity.ChatMessage;
import com.baton.chat.entity.MessageRole;
import com.baton.document.dto.KstTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * GET /chat/sessions/{id}/messages 항목 (API 설계서 5.2).
 * sources는 저장 시점에 Backend가 DB 권한으로 재검증한 JSON 배열 문자열이므로 그대로(raw) 내려준다.
 * USER 메시지는 sources가 없어 필드를 생략한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        @JsonRawValue String sources,
        String createdAt
) {

    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getRole(),
                m.getContent(),
                m.getSources(),
                KstTime.format(m.getCreatedAt())
        );
    }
}
