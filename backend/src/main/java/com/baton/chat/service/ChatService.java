package com.baton.chat.service;

import com.baton.chat.dto.ChatMessageResponse;
import com.baton.chat.dto.ChatSessionCreatedResponse;
import com.baton.chat.dto.ChatSessionResponse;
import com.baton.chat.entity.ChatSession;
import com.baton.chat.exception.ChatErrorCode;
import com.baton.chat.repository.ChatMessageRepository;
import com.baton.chat.repository.ChatSessionRepository;
import com.baton.common.BusinessException;
import com.baton.member.entity.Member;
import com.baton.security.AuthMember;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대화 세션·이력 (API 설계서 5.0·5.2). 질문 SSE 중계는 5단계에서 추가한다.
 * 모든 세션 접근은 "세션 ID + 내 회원 ID"로 조회 → 타인 세션은 404 SESSION_NOT_FOUND (IDOR 방어).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final EntityManager entityManager;

    /** GET /chat/sessions — 내 대화만, 최근 갱신 순 */
    public List<ChatSessionResponse> listSessions(AuthMember me) {
        return sessionRepository.findByMemberIdOrderByUpdatedAtDesc(me.id()).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    /** POST /chat/sessions — 제목 '새 대화'로 생성 (첫 질문 저장 시 5단계에서 갱신) */
    @Transactional
    public ChatSessionCreatedResponse createSession(AuthMember me) {
        // MemberRepository(A 소유)를 거치지 않고 FK 참조만 만든다 (SELECT 없이 프록시)
        Member memberRef = entityManager.getReference(Member.class, me.id());
        ChatSession saved = sessionRepository.save(ChatSession.create(memberRef));
        return ChatSessionCreatedResponse.from(saved);
    }

    /** GET /chat/sessions/{id}/messages — 소유자만, 시간순 전체 이력 */
    public List<ChatMessageResponse> getMessages(AuthMember me, Long sessionId) {
        ChatSession session = findOwnedSession(me, sessionId);
        return messageRepository.findBySessionIdOrderByIdAsc(session.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /** 내 세션만 반환. 없음·타인 세션을 같은 404로 응답 (5단계 질문 API에서도 사용) */
    public ChatSession findOwnedSession(AuthMember me, Long sessionId) {
        return sessionRepository.findByIdAndMemberId(sessionId, me.id())
                .orElseThrow(() -> {
                    // TODO(A): AuditLogger 머지 후 권한 밖 세션 접근 감사 로그로 교체
                    log.warn("[AUDIT] SESSION_NOT_FOUND_OR_DENIED memberUuid={} sessionId={}", me.uuid(), sessionId);
                    return new BusinessException(ChatErrorCode.SESSION_NOT_FOUND);
                });
    }
}
