package com.baton.chat.repository;

import com.baton.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** GET /chat/sessions/{id}/messages — 전체 이력 시간순 (idx_chat_messages_session 사용) */
    List<ChatMessage> findBySessionIdOrderByIdAsc(Long sessionId);

    /** AI에 넘길 최근 대화 6개 (최신순으로 받아 서비스에서 뒤집는다) */
    List<ChatMessage> findTop6BySessionIdOrderByIdDesc(Long sessionId);
}
