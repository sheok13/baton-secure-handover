package com.baton.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baton.ai.dto.AiChatEvent;
import com.baton.ai.dto.AiChatRequest;
import com.baton.ai.service.AiClient;
import com.baton.chat.entity.ChatMessage;
import com.baton.chat.entity.ChatSession;
import com.baton.chat.entity.MessageRole;
import com.baton.chat.exception.ChatErrorCode;
import com.baton.chat.repository.ChatMessageRepository;
import com.baton.chat.repository.ChatSessionRepository;
import com.baton.common.BusinessException;
import com.baton.document.repository.DocumentRepository;
import com.baton.document.service.AccessPolicy;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import com.baton.security.AuthMember;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

class ChatStreamServiceTest {

    private static final AuthMember SALES_STAFF = new AuthMember(1L, "u1", Department.SALES, Rank.STAFF);

    private ChatSessionRepository sessionRepository;
    private ChatMessageRepository messageRepository;
    private AiClient aiClient;
    private ChatStreamService service;
    private ChatSession session;

    /** 보낸 SSE 이벤트를 문자열로 모으고, 완료되면 latch를 내린다 */
    static class RecordingEmitter extends SseEmitter {
        final List<String> events = new ArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void send(SseEventBuilder builder) {
            StringBuilder sb = new StringBuilder();
            builder.build().forEach(d -> sb.append(d.getData()));
            events.add(sb.toString());
        }

        @Override
        public void complete() {
            done.countDown();
        }

        @Override
        public void completeWithError(Throwable ex) {
            done.countDown();
        }

        String all() {
            return String.join("", events);
        }
    }

    private static AiChatEvent token(String text) {
        return new AiChatEvent("token", "{\"text\":\"" + text + "\"}");
    }

    @BeforeEach
    void setUp() {
        sessionRepository = mock(ChatSessionRepository.class);
        messageRepository = mock(ChatMessageRepository.class);
        aiClient = mock(AiClient.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        when(documentRepository.findAllById(any())).thenReturn(List.of(
                SourceRevalidatorTest.doc(3, "거래처 관리 매뉴얼", Department.SALES, Rank.STAFF),
                SourceRevalidatorTest.doc(5, "거래처 계약 단가표", Department.SALES, Rank.LEADER)));

        service = new ChatStreamService(sessionRepository, messageRepository,
                new SourceRevalidator(documentRepository, new AccessPolicy()), aiClient,
                mock(PlatformTransactionManager.class), 5_000, 10_000);

        session = ChatSession.create(null);
        ReflectionTestUtils.setField(session, "id", 1L);
        when(sessionRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", m.getRole() == MessageRole.ASSISTANT ? 42L : 41L);
            return m;
        });
    }

    @Test
    @DisplayName("정상: token 중계 → 출처 재검증(5번 제거) → done 때 USER→ASSISTANT 순서로 저장 → done에 messageId, 첫 질문으로 제목 갱신")
    void happyPath() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of(
                ChatMessage.assistant(session, "이전 답변", "[]"),
                ChatMessage.user(session, "이전 질문")));
        when(aiClient.streamChat(any())).thenReturn(Flux.just(
                token("매월 25일까지 "), token("입니다."),
                new AiChatEvent("sources", "[{\"docId\":3,\"title\":\"x\",\"page\":null},{\"docId\":5,\"title\":\"y\",\"page\":null}]"),
                new AiChatEvent("done", "{}")));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "  정산 마감이 언제야?  ", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:token", "event:sources", "event:done", "messageId=42");

        // 저장은 done 수신 후 한 번에: USER → ASSISTANT 순서 (captor는 호출 순서대로 쌓인다)
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ChatMessage::getRole)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);

        ChatMessage user = saved.getAllValues().get(0);
        assertThat(user.getContent()).isEqualTo("정산 마감이 언제야?");

        ChatMessage assistant = saved.getAllValues().get(1);
        assertThat(assistant.getContent()).isEqualTo("매월 25일까지 입니다.");
        assertThat(assistant.getSources()).contains("\"docId\":3").doesNotContain("\"docId\":5");
        assertThat(session.getTitle()).isEqualTo("정산 마감이 언제야?");

        // AI에는 DB 회원 기준 필터와 시간순 이력을 보낸다
        ArgumentCaptor<AiChatRequest> req = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiClient).streamChat(req.capture());
        assertThat(req.getValue().filter().departments()).containsExactly(Department.SALES, Department.ALL);
        assertThat(req.getValue().history()).extracting(AiChatRequest.HistoryItem::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(req.getValue().question()).isEqualTo("정산 마감이 언제야?");
    }

    @Test
    @DisplayName("AI가 error 이벤트 → FE에 AI_UNAVAILABLE, 질문·답변 모두 저장 안 함")
    void aiErrorEvent() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(aiClient.streamChat(any())).thenReturn(Flux.just(token("부분 "),
                new AiChatEvent("error", "{\"code\":\"UPSTREAM_FAILED\",\"message\":\"x\"}")));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "질문", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:error", "AI_UNAVAILABLE").doesNotContain("event:done");
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("done 없이 스트림 종료 → error, 질문·답변 모두 저장 안 함")
    void streamCutWithoutDone() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(aiClient.streamChat(any())).thenReturn(Flux.just(token("끊김")));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "질문", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:error");
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("AI 연결 실패(Flux 오류) → error, 질문·답변 모두 저장 안 함")
    void aiConnectionFailure() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(aiClient.streamChat(any())).thenReturn(Flux.error(new IllegalStateException("connection refused")));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "질문", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:error", "AI_UNAVAILABLE");
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("타인 세션 → 스트림 시작 전에 404, 저장·AI 호출 없음")
    void otherSession() {
        when(sessionRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stream(SALES_STAFF, 9L, "질문", new RecordingEmitter()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.SESSION_NOT_FOUND);
        verify(messageRepository, never()).save(any());
        verify(aiClient, never()).streamChat(any());
    }

    @Test
    @DisplayName("AI token data가 JSON이 아님 → error 이벤트, 질문·답변 모두 저장 안 함")
    void malformedAiJson() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(aiClient.streamChat(any())).thenReturn(Flux.just(new AiChatEvent("token", "not-json")));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "질문", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:error", "AI_UNAVAILABLE");
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("done 후 DB 저장 실패 → done 대신 error를 보내고 스트림을 바로 닫는다 (타임아웃까지 매달리지 않음)")
    void saveFailureAfterDone() throws InterruptedException {
        when(messageRepository.findTop6BySessionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(aiClient.streamChat(any())).thenReturn(Flux.just(token("답변"), new AiChatEvent("done", "{}")));
        // when(...save(any())) 형태는 기존 thenAnswer를 null 인자로 호출해 NPE가 나므로 doThrow를 쓴다
        doThrow(new IllegalStateException("DB down")).when(messageRepository).save(any(ChatMessage.class));

        RecordingEmitter emitter = new RecordingEmitter();
        service.stream(SALES_STAFF, 1L, "질문", emitter);
        assertThat(emitter.done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(emitter.all()).contains("event:error", "AI_UNAVAILABLE").doesNotContain("event:done");
    }
}