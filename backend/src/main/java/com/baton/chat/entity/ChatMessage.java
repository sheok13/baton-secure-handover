package com.baton.chat.entity;

import com.baton.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 대화 메시지 (DB 설계서 3.4). sources는 재검증을 통과한 출처만 JSON 문자열로 저장한다. */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // V1: sources JSON NULL. validate에서 컬럼 타입 오류가 나면 @JdbcTypeCode(SqlTypes.JSON)을 추가한다
    @Column(columnDefinition = "json")
    private String sources;

    public static ChatMessage user(ChatSession session, String question) {
        ChatMessage m = new ChatMessage();
        m.session = session;
        m.role = MessageRole.USER;
        m.content = question;
        return m;
    }

    public static ChatMessage assistant(ChatSession session, String answer, String sourcesJson) {
        ChatMessage m = new ChatMessage();
        m.session = session;
        m.role = MessageRole.ASSISTANT;
        m.content = answer;
        m.sources = sourcesJson;
        return m;
    }
}
