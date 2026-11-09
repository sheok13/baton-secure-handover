package com.baton.chat.service;

import com.baton.ai.dto.AiChatEvent;
import com.baton.ai.dto.AiChatRequest;
import com.baton.ai.dto.AiChatRequest.HistoryItem;
import com.baton.ai.dto.SourceDto;
import com.baton.ai.exception.AiErrorCode;
import com.baton.ai.service.AiClient;
import com.baton.ai.service.AiFilter;
import com.baton.ai.service.AiJson;
import com.baton.chat.entity.ChatMessage;
import com.baton.chat.entity.ChatSession;
import com.baton.chat.exception.ChatErrorCode;
import com.baton.chat.repository.ChatMessageRepository;
import com.baton.chat.repository.ChatSessionRepository;
import com.baton.common.BusinessException;
import com.baton.security.AuthMember;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 질문 → AI SSE 중계 (API 설계서 5.1, Backend 명세 6장).
 * 처리 순서: 소유자 검증 → USER 저장(첫 질문이면 제목 갱신) → AI 호출·token 중계 → sources 재검증 후 중계
 * → done 수신 시 ASSISTANT 저장 → done(messageId) 전송.
 * AI 오류·타임아웃·done 없이 끊김 → error(AI_UNAVAILABLE) 전송, ASSISTANT는 저장하지 않는다.
 */
@Slf4j
@Service
public class ChatStreamService {

    private static final int HISTORY_SIZE = 6;
    private static final int HISTORY_CONTENT_MAX = 4000;   // ai-module HistoryMessage.content 최대 길이

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SourceRevalidator sourceRevalidator;
    private final AiClient aiClient;
    private final TransactionTemplate tx;
    private final Duration firstTokenTimeout;
    private final Duration streamTimeout;

    public ChatStreamService(ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository,
                             SourceRevalidator sourceRevalidator,
                             AiClient aiClient,
                             PlatformTransactionManager transactionManager,
                             @Value("${ai.chat-first-token-timeout-ms:30000}") long firstTokenTimeoutMs,
                             @Value("${ai.chat-stream-timeout-ms:120000}") long streamTimeoutMs) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.sourceRevalidator = sourceRevalidator;
        this.aiClient = aiClient;
        this.tx = new TransactionTemplate(transactionManager);
        this.firstTokenTimeout = Duration.ofMillis(firstTokenTimeoutMs);
        this.streamTimeout = Duration.ofMillis(streamTimeoutMs);
    }

    /** 컨트롤러 진입점. 검증 오류(404 등)는 스트림 시작 전에 일반 JSON 오류로 응답된다 */
    public SseEmitter ask(AuthMember me, Long sessionId, String question) {
        SseEmitter emitter = new SseEmitter(streamTimeout.toMillis() + 10_000);
        stream(me, sessionId, question, emitter);
        return emitter;
    }

    void stream(AuthMember me, Long sessionId, String rawQuestion, SseEmitter emitter) {
        String question = rawQuestion.strip();

        // ① 소유자 검증 + 이전 대화(최근 6개) + USER 저장 + 제목 갱신 — 짧은 트랜잭션
        Prepared prepared = tx.execute(status -> {
            ChatSession session = sessionRepository.findByIdAndMemberId(sessionId, me.id())
                    .orElseThrow(() -> {
                        // TODO(A): AuditLogger 머지 후 권한 밖 세션 접근 감사 로그로 교체
                        log.warn("[AUDIT] SESSION_NOT_FOUND_OR_DENIED memberUuid={} sessionId={}", me.uuid(), sessionId);
                        return new BusinessException(ChatErrorCode.SESSION_NOT_FOUND);
                    });
            List<HistoryItem> history = recentHistory(session.getId());
            messageRepository.save(ChatMessage.user(session, question));
            session.updateTitleIfDefault(question);
            session.touch();
            return new Prepared(session.getId(), history);
        });

        // ② 권한 필터는 DB 회원 정보로만 계산 (요청 본문 값 사용 안 함)
        AiChatRequest request = new AiChatRequest(question, AiFilter.of(me.department(), me.rank()), prepared.history());

        StringBuilder answer = new StringBuilder();
        AtomicReference<List<SourceDto>> sources = new AtomicReference<>(List.of());
        AtomicBoolean finished = new AtomicBoolean(false);

        // ③ AI 스트림 구독 — DB 작업이 있으므로 Netty 스레드가 아닌 boundedElastic에서 처리
        Disposable subscription = aiClient.streamChat(request)
                .timeout(Mono.delay(firstTokenTimeout), ev -> Mono.delay(firstTokenTimeout))  // 첫 이벤트·이벤트 간격 30초
                .take(streamTimeout)                                                          // 전체 120초
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        ev -> handle(me, prepared.sessionId(), ev, answer, sources, finished, emitter),
                        err -> {
                            log.warn("AI 채팅 스트림 오류 sessionId={} reason={}", prepared.sessionId(), err.toString());
                            if (finished.compareAndSet(false, true)) {
                                sendErrorAndComplete(emitter);
                            }
                        },
                        () -> {
                            if (finished.compareAndSet(false, true)) {
                                log.warn("AI 채팅 스트림이 done 없이 종료 sessionId={}", prepared.sessionId());
                                sendErrorAndComplete(emitter);
                            }
                        });

        // 클라이언트 연결 종료·타임아웃 시 AI 스트림도 끊는다 (불완전한 답변은 저장하지 않음)
        emitter.onTimeout(() -> {
            subscription.dispose();
            if (finished.compareAndSet(false, true)) {
                sendErrorAndComplete(emitter);
            }
        });
        emitter.onError(e -> subscription.dispose());
        emitter.onCompletion(subscription::dispose);
    }

    private void handle(AuthMember me, Long sessionId, AiChatEvent ev, StringBuilder answer,
                        AtomicReference<List<SourceDto>> sources, AtomicBoolean finished, SseEmitter emitter) {
        if (finished.get()) {
            return;
        }
        try {
            switch (ev.event()) {
                case AiChatEvent.TOKEN -> {
                    String text = AiJson.MAPPER.readValue(ev.data(), TokenData.class).text();
                    if (text != null && !text.isEmpty()) {
                        answer.append(text);
                        send(emitter, AiChatEvent.TOKEN, Map.of("text", text));
                    }
                }
                case AiChatEvent.SOURCES -> {
                    List<SourceDto> fromAi = List.of(AiJson.MAPPER.readValue(ev.data(), SourceDto[].class));
                    List<SourceDto> allowed = sourceRevalidator.filter(me, fromAi);
                    sources.set(allowed);
                    send(emitter, AiChatEvent.SOURCES, allowed);
                }
                case AiChatEvent.DONE -> {
                    if (!finished.compareAndSet(false, true)) {
                        return;
                    }
                    Long messageId = saveAssistant(sessionId, answer.toString(), sources.get());
                    send(emitter, AiChatEvent.DONE, Map.of("messageId", messageId));
                    emitter.complete();
                }
                case AiChatEvent.ERROR -> {
                    if (finished.compareAndSet(false, true)) {
                        log.warn("AI가 error 이벤트 전송 sessionId={} data={}", sessionId, ev.data());
                        sendErrorAndComplete(emitter);
                    }
                }
                default -> log.debug("알 수 없는 AI 이벤트 무시 event={}", ev.event());
            }
        } catch (IOException e) {
            // 클라이언트가 연결을 끊음 → 더 보낼 곳이 없으므로 종료 (ASSISTANT 미저장)
            log.info("클라이언트 연결 종료 sessionId={}", sessionId);
            finished.set(true);
            emitter.completeWithError(e);
        } catch (RuntimeException e) {
            log.warn("AI 이벤트 처리 실패 sessionId={} event={} reason={}", sessionId, ev.event(), e.toString());
            if (finished.compareAndSet(false, true)) {
                sendErrorAndComplete(emitter);
            }
        }
    }

    /** ASSISTANT 저장 + 세션 updatedAt 갱신. 저장된 messageId를 done에 붙인다 */
    private Long saveAssistant(Long sessionId, String answer, List<SourceDto> sources) {
        String sourcesJson;
        try {
            sourcesJson = AiJson.MAPPER.writeValueAsString(sources);
        } catch (Exception e) {
            sourcesJson = "[]";
        }
        final String finalSourcesJson = sourcesJson;
        return tx.execute(status -> {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalStateException("세션이 사라졌습니다 sessionId=" + sessionId));
            ChatMessage saved = messageRepository.save(ChatMessage.assistant(session, answer, finalSourcesJson));
            session.touch();
            return saved.getId();
        });
    }

    /** 이번 질문 저장 전의 최근 6개를 시간순으로 (AI 명세 4.2 history) */
    private List<HistoryItem> recentHistory(Long sessionId) {
        List<ChatMessage> latest = new ArrayList<>(messageRepository.findTop6BySessionIdOrderByIdDesc(sessionId));
        Collections.reverse(latest);
        return latest.stream()
                .limit(HISTORY_SIZE)
                .map(m -> new HistoryItem(m.getRole(), truncate(m.getContent())))
                .toList();
    }

    private static String truncate(String s) {
        return s.length() > HISTORY_CONTENT_MAX ? s.substring(0, HISTORY_CONTENT_MAX) : s;
    }

    private static void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
    }

    private static void sendErrorAndComplete(SseEmitter emitter) {
        try {
            send(emitter, AiChatEvent.ERROR, Map.of(
                    "code", AiErrorCode.AI_UNAVAILABLE.getCode(),
                    "message", AiErrorCode.AI_UNAVAILABLE.getMessage()));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private record Prepared(Long sessionId, List<HistoryItem> history) {
    }

    record TokenData(String text) {
    }
}
