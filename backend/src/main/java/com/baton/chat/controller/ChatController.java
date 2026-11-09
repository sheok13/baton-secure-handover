package com.baton.chat.controller;

import com.baton.chat.dto.ChatMessageResponse;
import com.baton.chat.dto.ChatSessionCreatedResponse;
import com.baton.chat.dto.ChatSessionResponse;
import com.baton.chat.service.ChatService;
import com.baton.common.ApiResponse;
import com.baton.security.AuthMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 업무 채팅 API (API 설계서 5장). 질문 SSE(POST /{id}/messages)는 5단계에서 추가 */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** GET /api/chat/sessions */
    @GetMapping
    public ApiResponse<List<ChatSessionResponse>> list(@AuthenticationPrincipal AuthMember me) {
        return ApiResponse.success(chatService.listSessions(me));
    }

    /** POST /api/chat/sessions — 요청 본문 없음, 201 */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatSessionCreatedResponse>> create(@AuthenticationPrincipal AuthMember me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(chatService.createSession(me)));
    }

    /** GET /api/chat/sessions/{id}/messages */
    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(
            @AuthenticationPrincipal AuthMember me,
            @PathVariable Long id) {
        return ApiResponse.success(chatService.getMessages(me, id));
    }
}
