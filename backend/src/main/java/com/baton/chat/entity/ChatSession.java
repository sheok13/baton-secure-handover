package com.baton.chat.entity;

import com.baton.common.BaseTimeEntity;
import com.baton.member.entity.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화방 (DB 설계서 3.3).
 * BaseTimeEntity는 createdAt만 가지므로 updatedAt과 touch()는 이 엔티티에만 둔다.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseTimeEntity {

    public static final String DEFAULT_TITLE = "새 대화";
    private static final int TITLE_FROM_QUESTION_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // DB DEFAULT '새 대화'는 JPA INSERT(모든 컬럼 전송)에 적용되지 않으므로 필드 초기값으로도 넣는다
    @Column(nullable = false, length = 100)
    private String title = DEFAULT_TITLE;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // DB 설계서 4장: 세션 삭제 시 메시지 함께 삭제 (DB도 ON DELETE CASCADE)
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatSession create(Member member) {
        ChatSession s = new ChatSession();
        s.member = member;
        return s;
    }

    @PrePersist
    void initUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    /** 메시지 저장 시 호출 → 대화 목록 최신순 정렬 기준 갱신 */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 첫 질문이면 제목을 질문 앞 30자로 바꾼다 (이미 바뀐 제목은 유지) */
    public void updateTitleIfDefault(String question) {
        if (!DEFAULT_TITLE.equals(title) || question == null || question.isBlank()) {
            return;
        }
        String q = question.strip().replaceAll("\\s+", " ");
        this.title = q.length() > TITLE_FROM_QUESTION_LENGTH
                ? q.substring(0, TITLE_FROM_QUESTION_LENGTH)
                : q;
    }

    /** IDOR 방어: 소유자 검증 */
    public boolean isOwnedBy(Long memberId) {
        return member != null && member.getId() != null && member.getId().equals(memberId);
    }
}
