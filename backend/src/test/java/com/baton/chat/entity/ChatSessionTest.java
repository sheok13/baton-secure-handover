package com.baton.chat.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baton.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1단계에서 만든 ChatSession 동작 검증 (제목 자동 생성·touch·소유자) */
class ChatSessionTest {

    @Test
    @DisplayName("생성 시 제목은 '새 대화'")
    void defaultTitle() {
        assertThat(ChatSession.create(null).getTitle()).isEqualTo(ChatSession.DEFAULT_TITLE);
    }

    @Test
    @DisplayName("첫 질문으로 제목 갱신: 앞뒤 공백 제거, 연속 공백·줄바꿈은 한 칸")
    void titleFromFirstQuestion() {
        ChatSession s = ChatSession.create(null);
        s.updateTitleIfDefault("  정산   마감이\n언제야?  ");
        assertThat(s.getTitle()).isEqualTo("정산 마감이 언제야?");
    }

    @Test
    @DisplayName("30자를 넘는 질문은 앞 30자만 제목으로")
    void titleTruncatedTo30() {
        ChatSession s = ChatSession.create(null);
        s.updateTitleIfDefault("가".repeat(40));
        assertThat(s.getTitle()).hasSize(30);
    }

    @Test
    @DisplayName("두 번째 질문부터는 제목을 바꾸지 않는다")
    void titleOnlyOnce() {
        ChatSession s = ChatSession.create(null);
        s.updateTitleIfDefault("첫 질문");
        s.updateTitleIfDefault("두 번째 질문");
        assertThat(s.getTitle()).isEqualTo("첫 질문");
    }

    @Test
    @DisplayName("빈 질문은 제목을 바꾸지 않는다")
    void blankQuestionIgnored() {
        ChatSession s = ChatSession.create(null);
        s.updateTitleIfDefault("   ");
        assertThat(s.getTitle()).isEqualTo(ChatSession.DEFAULT_TITLE);
    }

    @Test
    @DisplayName("touch()는 updatedAt을 갱신한다")
    void touchUpdatesTime() {
        ChatSession s = ChatSession.create(null);
        assertThat(s.getUpdatedAt()).isNull();
        s.touch();
        assertThat(s.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("소유자 검증: 내 회원 ID만 true")
    void isOwnedBy() {
        Member owner = mock(Member.class);
        when(owner.getId()).thenReturn(1L);
        ChatSession s = ChatSession.create(owner);

        assertThat(s.isOwnedBy(1L)).isTrue();
        assertThat(s.isOwnedBy(2L)).isFalse();
        assertThat(ChatSession.create(null).isOwnedBy(1L)).isFalse();
    }
}
