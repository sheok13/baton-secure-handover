package com.baton.chat.controller;

import com.baton.chat.dto.ChatMessageResponse;
import com.baton.chat.dto.ChatQuestionRequest;
import com.baton.chat.dto.ChatSessionCreatedResponse;
import com.baton.chat.dto.ChatSessionResponse;
import com.baton.chat.service.ChatService;
import com.baton.chat.service.ChatStreamService;
import com.baton.common.ApiResponse;
import com.baton.security.AuthMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 업무 채팅 API (API 설계서 5장) */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;

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

    /**
     * POST /api/chat/sessions/{id}/messages — text/event-stream (token → sources → done / error).
     * produces를 지정하지 않는다: 질문 검증 실패(400)·타인 세션(404)은 스트림 시작 전에 JSON 오류로 응답해야 하기 때문.
     */
    @PostMapping("/{id}/messages")
    public SseEmitter ask(
            @AuthenticationPrincipal AuthMember me,
            @PathVariable Long id,
            @Valid @RequestBody ChatQuestionRequest request) {
        return chatStreamService.ask(me, id, request.question());
    }
}
