package com.baton.chat.repository;

import com.baton.chat.entity.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /** GET /chat/sessions — 내 대화만, updatedAt 내림차순 (idx_chat_sessions_member 사용) */
    List<ChatSession> findByMemberIdOrderByUpdatedAtDesc(Long memberId);

    /** 세션 ID + 소유자로 조회 → 타인 세션이면 empty → 404 SESSION_NOT_FOUND (IDOR 방어) */
    Optional<ChatSession> findByIdAndMemberId(Long id, Long memberId);
}
