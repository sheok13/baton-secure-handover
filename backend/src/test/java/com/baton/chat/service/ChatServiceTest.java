package com.baton.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baton.chat.dto.ChatMessageResponse;
import com.baton.chat.dto.ChatSessionCreatedResponse;
import com.baton.chat.dto.ChatSessionResponse;
import com.baton.chat.entity.ChatMessage;
import com.baton.chat.entity.ChatSession;
import com.baton.chat.entity.MessageRole;
import com.baton.chat.exception.ChatErrorCode;
import com.baton.chat.repository.ChatMessageRepository;
import com.baton.chat.repository.ChatSessionRepository;
import com.baton.common.BusinessException;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import com.baton.security.AuthMember;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static final AuthMember ME = new AuthMember(1L, "uuid-sales-staff", Department.SALES, Rank.STAFF);

    private ChatSessionRepository sessionRepository;
    private ChatMessageRepository messageRepository;
    private EntityManager entityManager;
    private ChatService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(ChatSessionRepository.class);
        messageRepository = mock(ChatMessageRepository.class);
        entityManager = mock(EntityManager.class);
        service = new ChatService(sessionRepository, messageRepository, entityManager);
    }

    @Test
    @DisplayName("대화 목록: 내 회원 ID로만 조회")
    void listSessions() {
        ChatSession s = ChatSession.create(null);
        s.updateTitleIfDefault("정산 마감이 언제야?");
        when(sessionRepository.findByMemberIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(s));

        List<ChatSessionResponse> res = service.listSessions(ME);

        assertThat(res).extracting(ChatSessionResponse::title).containsExactly("정산 마감이 언제야?");
        verify(sessionRepository).findByMemberIdOrderByUpdatedAtDesc(1L);
    }

    @Test
    @DisplayName("새 대화: 내 회원 참조로 생성, 제목 '새 대화'")
    void createSession() {
        Member ref = mock(Member.class);
        when(entityManager.getReference(Member.class, 1L)).thenReturn(ref);
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatSessionCreatedResponse res = service.createSession(ME);

        assertThat(res.title()).isEqualTo(ChatSession.DEFAULT_TITLE);
        verify(entityManager).getReference(Member.class, 1L);
    }

    @Test
    @DisplayName("타인 세션 이력 조회(IDOR) → 404 SESSION_NOT_FOUND")
    void otherMembersSessionIs404() {
        when(sessionRepository.findByIdAndMemberId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessages(ME, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("내 세션 이력: 시간순, USER는 sources 없음 / ASSISTANT는 저장된 출처 JSON")
    void ownSessionMessages() {
        ChatSession s = ChatSession.create(null);
        when(sessionRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(s));
        String sources = "[{\"docId\":3,\"title\":\"거래처 관리 매뉴얼\",\"page\":null}]";
        when(messageRepository.findBySessionIdOrderByIdAsc(any())).thenReturn(List.of(
                ChatMessage.user(s, "정산 마감이 언제야?"),
                ChatMessage.assistant(s, "매월 25일까지입니다.", sources)));

        List<ChatMessageResponse> res = service.getMessages(ME, 1L);

        assertThat(res).extracting(ChatMessageResponse::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(res.get(0).sources()).isNull();
        assertThat(res.get(1).sources()).isEqualTo(sources);
    }
}
